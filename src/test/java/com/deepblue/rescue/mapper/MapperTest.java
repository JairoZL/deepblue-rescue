package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica las implementaciones que MapStruct genera en compilación,
 * en especial los @Mapping de propiedades anidadas. Sin Spring ni base de datos.
 */
class MapperTest {

    private final RescueCaseMapper rescueCaseMapper = Mappers.getMapper(RescueCaseMapper.class);
    private final TreatmentMapper treatmentMapper = Mappers.getMapper(TreatmentMapper.class);
    private final AnimalMapper animalMapper = Mappers.getMapper(AnimalMapper.class);

    @Test
    void shouldMapRescueCaseWithNestedCodes() {
        Animal animal = animal();
        RescueCase rescueCase = animal.getRescueCase();
        rescueCase.setId(1L);

        RescueCaseResponse response = rescueCaseMapper.toResponse(rescueCase);

        assertThat(response).isEqualTo(new RescueCaseResponse(1L, "RES-2026-100",
                LocalDate.of(2026, 8, 20), "Bahía de Taganga", RescueStatus.IN_REHABILITATION,
                "CTR-001", "AN-2026-100"));
    }

    @Test
    void shouldMapRescueCaseWithoutAnimalAsNullCode() {
        RescueCase rescueCase = new RescueCase("RES-002", LocalDate.of(2026, 9, 1), "Rodadero", RescueStatus.ADMITTED);

        assertThat(rescueCaseMapper.toResponse(rescueCase).animalCode()).isNull();
    }

    @Test
    void shouldMapTreatmentWithNestedCodes() {
        Specialist specialist = new Specialist("SPEC-001", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        LocalDateTime performedAt = LocalDateTime.of(2026, 8, 21, 9, 0);
        Treatment treatment = new Treatment(animal(), specialist, performedAt,
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");
        treatment.setId(10L);

        TreatmentResponse response = treatmentMapper.toResponse(treatment);

        assertThat(response).isEqualTo(new TreatmentResponse(10L, "AN-2026-100", "SPEC-001",
                performedAt, TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury."));
    }

    @Test
    void shouldMapAnimalWithRescueCaseData() {
        Animal animal = animal();
        animal.setId(5L);

        AnimalResponse response = animalMapper.toResponse(animal);

        assertThat(response).isEqualTo(new AnimalResponse(5L, "AN-2026-100", "Green Sea Turtle",
                "Chelonia mydas", AnimalSex.FEMALE, "RES-2026-100", RescueStatus.IN_REHABILITATION));
    }

    private static Animal animal() {
        RescueCenter center = new RescueCenter("CTR-001", "DeepBlue Caribe", "Santa Marta");
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 20),
                "Bahía de Taganga", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        return animal;
    }
}
