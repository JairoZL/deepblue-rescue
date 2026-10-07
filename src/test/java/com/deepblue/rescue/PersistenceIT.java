package com.deepblue.rescue;


import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.MedicalRecord;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PersistenceIT {

    @Autowired
    private RescueCenterRepository rescueCenterRepository;

    @Autowired
    private RescueCaseRepository rescueCaseRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private SpecialistRepository specialistRepository;

    @Autowired
    private ExpertiseRepository expertiseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    // Paso 47
    @Test
    void flywayMigrationsWereApplied() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true",
                String.class
        );

        assertThat(versions).contains("1", "2");
    }

    // Paso 48
    @Test
    void inheritedMethodsWorkForRescueCenter() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");

        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();

        Optional<RescueCenter> found = rescueCenterRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("DB-CAR");

        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();

        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    // Paso 49 - relacion 1:N RescueCenter -> RescueCase
    @Test
    void oneToManyRescueCenterAndRescueCases(){
        RescueCenter center= new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter saved = rescueCenterRepository.save(center);
        assertThat(saved.getId()).isNotNull();

        RescueCase case1 = new RescueCase("RES-2026-001", LocalDate.of(2026, 9, 10), "Santa Marta", RescueStatus.ADMITTED);
        case1.setRescueCenter(saved);

        RescueCase case2 = new RescueCase("RES-2026-002", LocalDate.of(2026, 9, 15), "Santa Marta", RescueStatus.ADMITTED);
        case2.setRescueCenter(saved);

        RescueCase savedCase1 = rescueCaseRepository.save(case1);
        RescueCase savedCase2 = rescueCaseRepository.save(case2);

        assertThat(savedCase1.getRescueCenter().getId()).isEqualTo(saved.getId());
        assertThat(savedCase2.getRescueCenter().getId()).isEqualTo(saved.getId());
        assertThat(savedCase1.getRescueCenter().getCode())
                .isEqualTo(savedCase2.getRescueCenter().getCode());
    }

    // Paso 50 - relacion 1:1 RescueCase <-> Animal
    @Test
    void oneToOneRescueCaseAndAnimal() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-003", LocalDate.of(2026, 9, 18), "Santa Marta", RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        RescueCase savedCase = rescueCaseRepository.save(rescueCase);

        assertThat(savedCase.getAnimal()).isNotNull();
        assertThat(savedCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(savedCase.getAnimal().getRescueCase().getCaseCode()).isEqualTo(savedCase.getCaseCode());
    }

    // Paso 51 - relacion 1:1 Animal <-> MedicalRecord, persistida en cascada
    @Test
    void oneToOneAnimalAndMedicalRecordWithCascade() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-004", LocalDate.of(2026, 9, 20), "Santa Marta", RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(new BigDecimal("28.40"), "STABLE", "Left front flipper injury", null);
        animal.assignMedicalRecord(record);

        // Un solo save: cascade ALL propaga RescueCase -> Animal -> MedicalRecord
        RescueCase savedCase = rescueCaseRepository.save(rescueCase);

        assertThat(savedCase.getAnimal().getId()).isNotNull();
        assertThat(savedCase.getAnimal().getMedicalRecord()).isNotNull();
        assertThat(savedCase.getAnimal().getMedicalRecord().getId()).isNotNull();
    }

    // Paso 52 - relacion N:M Specialist <-> Expertise
    @Test
    void manyToManySpecialistAndExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);

        Specialist savedElena = specialistRepository.save(elena);

        // consulta indirecta de specialist_expertise mediante la entidad
        List<Specialist> withTrauma = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(withTrauma).extracting(Specialist::getProfessionalCode).contains("SPEC-001");

        assertThat(savedElena.getExpertiseAreas()).hasSize(2);
    }

    // Paso 53 - Query Method simple (status)
    @Test
    void queryMethodByStatus() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase case1 = new RescueCase("RES-001", LocalDate.of(2026, 1, 10), "Santa Marta", RescueStatus.IN_REHABILITATION);
        case1.setRescueCenter(center);
        RescueCase case2 = new RescueCase("RES-002", LocalDate.of(2026, 1, 12), "Santa Marta", RescueStatus.READY_FOR_RELEASE);
        case2.setRescueCenter(center);
        RescueCase case3 = new RescueCase("RES-003", LocalDate.of(2026, 1, 15), "Santa Marta", RescueStatus.IN_REHABILITATION);
        case3.setRescueCenter(center);

        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);
        rescueCaseRepository.save(case3);

        List<RescueCase> inRehabilitation = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(inRehabilitation).hasSize(2);
        assertThat(inRehabilitation).extracting(RescueCase::getCaseCode)
                .containsExactly("RES-001", "RES-003");
    }

    // Paso 54 - Query Method navegando relaciones (animales de determinado centro)
    @Test
    void queryMethodNavigatingRelations() {
        RescueCenter carCenter = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));
        RescueCenter pacCenter = rescueCenterRepository.save(
                new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Buenaventura"));

        RescueCase carCase = new RescueCase("RES-CAR-001", LocalDate.of(2026, 2, 1), "Santa Marta", RescueStatus.ADMITTED);
        carCase.setRescueCenter(carCenter);
        Animal carAnimal = new Animal("AN-CAR-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        carCase.assignAnimal(carAnimal);
        rescueCaseRepository.save(carCase);

        RescueCase pacCase = new RescueCase("RES-PAC-001", LocalDate.of(2026, 2, 2), "Buenaventura", RescueStatus.ADMITTED);
        pacCase.setRescueCenter(pacCenter);
        Animal pacAnimal = new Animal("AN-PAC-001", "Olive Ridley Turtle", "Lepidochelys olivacea", AnimalSex.MALE);
        pacCase.assignAnimal(pacAnimal);
        rescueCaseRepository.save(pacCase);

        List<Animal> carAnimals = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(carAnimals).extracting(Animal::getAnimalCode).containsExactly("AN-CAR-001");
        assertThat(carAnimals).extracting(Animal::getAnimalCode).doesNotContain("AN-PAC-001");
    }

    // Paso 55 - JPQL de especialistas por expertise
    @Test
    void jpqlFindActiveByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise marineBirds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SPEC-010", "Elena", "Vargas", "elena.10@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SPEC-011", "Mateo", "Rojas", "mateo.11@deepblue.org", true);
        mateo.addExpertise(marineMammals);
        mateo.addExpertise(rehabilitation);
        specialistRepository.save(mateo);

        Specialist sofia = new Specialist("SPEC-012", "Sofia", "Castro", "sofia.12@deepblue.org", true);
        sofia.addExpertise(marineBirds);
        sofia.addExpertise(trauma);
        specialistRepository.save(sofia);

        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(traumaSpecialists).extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    // Pasos 56-57 - crear tratamientos y Query Method ordenado cronologicamente
    @Test
    void queryMethodTreatmentsOrderedChronologically() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-010", LocalDate.of(2026, 3, 1), "Santa Marta", RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);
        Animal animal = new Animal("AN-2026-010", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.save(rescueCase);

        Specialist elena = specialistRepository.save(new Specialist("SPEC-020", "Elena", "Vargas", "elena.20@deepblue.org", true));
        Specialist mateo = specialistRepository.save(new Specialist("SPEC-021", "Mateo", "Rojas", "mateo.21@deepblue.org", true));

        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 3, 1, 9, 0), TreatmentType.WOUND_CARE, "Treatment 1"));
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 3, 2, 9, 0), TreatmentType.HYDRATION, "Treatment 2"));
        treatmentRepository.save(new Treatment(animal, mateo, LocalDateTime.of(2026, 3, 3, 9, 0), TreatmentType.OBSERVATION, "Treatment 3"));

        List<Treatment> treatments = treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-2026-010");

        assertThat(treatments).extracting(Treatment::getDescription)
                .containsExactly("Treatment 1", "Treatment 2", "Treatment 3");
    }

    // Paso 58 - JPQL por intervalo de fechas
    @Test
    void jpqlTreatmentsByInterval() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-011", LocalDate.of(2026, 8, 1), "Santa Marta", RescueStatus.ADMITTED);
        rescueCase.setRescueCenter(center);
        Animal animal = new Animal("AN-2026-011", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.save(rescueCase);

        Specialist elena = specialistRepository.save(new Specialist("SPEC-030", "Elena", "Vargas", "elena.30@deepblue.org", true));

        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Early treatment"));
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Mid treatment"));
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Late treatment"));

        List<Treatment> inRange = treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0)
        );

        assertThat(inRange).hasSize(1);
        assertThat(inRange.get(0).getDescription()).isEqualTo("Mid treatment");
    }

    // Paso 59 - constraint UNIQUE sobre animal_code
    @Test
    void uniqueConstraintOnAnimalCode() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta"));

        RescueCase case1 = new RescueCase("RES-UQ-001", LocalDate.of(2026, 4, 1), "Santa Marta", RescueStatus.ADMITTED);
        case1.setRescueCenter(center);
        Animal animal1 = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        case1.assignAnimal(animal1);
        rescueCaseRepository.saveAndFlush(case1);

        RescueCase case2 = new RescueCase("RES-UQ-002", LocalDate.of(2026, 4, 2), "Santa Marta", RescueStatus.ADMITTED);
        case2.setRescueCenter(center);
        Animal animal2 = new Animal("AN-100", "Olive Ridley Turtle", "Lepidochelys olivacea", AnimalSex.MALE);
        case2.assignAnimal(animal2);

        assertThatThrownBy(() -> rescueCaseRepository.saveAndFlush(case2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Pasos 65-66 - Reto integrador: escenario completo de la tortuga y sus 8 consultas
    @Test
    void integratorScenarioAndQueries() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 18), "Bahia Concha", RescueStatus.IN_REHABILITATION);
        rescueCase.setRescueCenter(center);

        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(new BigDecimal("27.80"), "STABLE",
                "Injury caused by fishing net", "Possible plastic ingestion");
        animal.assignMedicalRecord(record);

        rescueCaseRepository.save(rescueCase);

        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-100", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 18, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper"));
        treatmentRepository.save(new Treatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy"));

        // Consulta 1: existe el caso? -> metodo nuevo existsByCaseCode
        assertThat(rescueCaseRepository.existsByCaseCode("RES-2026-100")).isTrue();

        // Consulta 2: casos IN_REHABILITATION
        assertThat(rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .extracting(RescueCase::getCaseCode).contains("RES-2026-100");

        // Consulta 3: animales del centro DB-CAR
        assertThat(animalRepository.findByRescueCaseRescueCenterCode("DB-CAR"))
                .extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 4: nombre comun contiene "turtle" ignorando mayusculas
        assertThat(animalRepository.findByCommonNameContainingIgnoreCase("turtle"))
                .extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 5: especialistas con expertise Trauma
        assertThat(specialistRepository.findActiveByExpertise("Trauma"))
                .extracting(Specialist::getProfessionalCode).contains("SPEC-100");

        // Consulta 6: tratamientos del animal, ordenados cronologicamente
        assertThat(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc("AN-2026-100"))
                .extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 7: tratamientos realizados por especialistas con expertise Rehabilitation
        assertThat(treatmentRepository.findBySpecialistExpertise("Rehabilitation"))
                .extracting(Treatment::getDescription)
                .contains("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 8: tratamientos entre dos fechas
        assertThat(treatmentRepository.findByPerformedAtBetween(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 23, 59)))
                .hasSize(2);
    }

    // Parte XIII - Reto sin guia: animales en rehabilitacion tratados por
    // al menos un especialista con expertise en Trauma, sin duplicados.
    @Test
    void customQueryAnimalsInRehabilitationTreatedByTraumaSpecialist() {
        RescueCenter center = rescueCenterRepository.save(
                new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta"));

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise marineMammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();

        Specialist traumaSpecialist = new Specialist("SPEC-200", "Carlos", "Ibarra", "carlos.ibarra@deepblue.org", true);
        traumaSpecialist.addExpertise(trauma);
        specialistRepository.save(traumaSpecialist);

        Specialist otherSpecialist = new Specialist("SPEC-201", "Ana", "Torres", "ana.torres@deepblue.org", true);
        otherSpecialist.addExpertise(marineMammals);
        specialistRepository.save(otherSpecialist);

        // Coincide: en rehabilitacion + 2 tratamientos por especialista con Trauma (prueba DISTINCT)
        RescueCase matchCase = new RescueCase("RES-R-001", LocalDate.of(2026, 5, 1), "Santa Marta", RescueStatus.IN_REHABILITATION);
        matchCase.setRescueCenter(center);
        Animal matchAnimal = new Animal("AN-R-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        matchCase.assignAnimal(matchAnimal);
        rescueCaseRepository.save(matchCase);
        treatmentRepository.save(new Treatment(matchAnimal, traumaSpecialist, LocalDateTime.of(2026, 5, 1, 9, 0), TreatmentType.WOUND_CARE, "First"));
        treatmentRepository.save(new Treatment(matchAnimal, traumaSpecialist, LocalDateTime.of(2026, 5, 2, 9, 0), TreatmentType.OBSERVATION, "Second"));

        // No coincide: status distinto a IN_REHABILITATION
        RescueCase wrongStatusCase = new RescueCase("RES-R-002", LocalDate.of(2026, 5, 1), "Santa Marta", RescueStatus.READY_FOR_RELEASE);
        wrongStatusCase.setRescueCenter(center);
        Animal wrongStatusAnimal = new Animal("AN-R-002", "Olive Ridley Turtle", "Lepidochelys olivacea", AnimalSex.MALE);
        wrongStatusCase.assignAnimal(wrongStatusAnimal);
        rescueCaseRepository.save(wrongStatusCase);
        treatmentRepository.save(new Treatment(wrongStatusAnimal, traumaSpecialist, LocalDateTime.of(2026, 5, 1, 9, 0), TreatmentType.WOUND_CARE, "Third"));

        // No coincide: tratado por especialista sin expertise en Trauma
        RescueCase wrongExpertiseCase = new RescueCase("RES-R-003", LocalDate.of(2026, 5, 1), "Santa Marta", RescueStatus.IN_REHABILITATION);
        wrongExpertiseCase.setRescueCenter(center);
        Animal wrongExpertiseAnimal = new Animal("AN-R-003", "Loggerhead Turtle", "Caretta caretta", AnimalSex.MALE);
        wrongExpertiseCase.assignAnimal(wrongExpertiseAnimal);
        rescueCaseRepository.save(wrongExpertiseCase);
        treatmentRepository.save(new Treatment(wrongExpertiseAnimal, otherSpecialist, LocalDateTime.of(2026, 5, 1, 9, 0), TreatmentType.OBSERVATION, "Fourth"));

        List<Animal> result = animalRepository.findByStatusAndTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        // Solo AN-R-001 cumple ambas condiciones, y aparece una sola vez (DISTINCT)
        assertThat(result).extracting(Animal::getAnimalCode).containsExactly("AN-R-001");
    }
}
