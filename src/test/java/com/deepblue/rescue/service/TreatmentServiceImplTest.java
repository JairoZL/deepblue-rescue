package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.impl.TreatmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    private static final String ANIMAL_CODE = "AN-001";
    private static final String SPECIALIST_CODE = "SPEC-001";
    private static final LocalDate RESCUE_DATE = LocalDate.of(2026, 8, 20);
    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2026, 8, 21, 9, 0);

    @Mock
    private AnimalRepository animalRepository;

    @Mock
    private SpecialistRepository specialistRepository;

    @Mock
    private TreatmentRepository treatmentRepository;

    @Mock
    private TreatmentMapper mapper;

    @InjectMocks
    private TreatmentServiceImpl service;

    // TEST 5
    @Test
    void shouldRegisterTreatmentWhenAllRulesPass() {
        Animal animal = animalWithCase(RescueStatus.IN_REHABILITATION);
        Specialist specialist = specialist(true);
        CreateTreatmentRequest request = request(TreatmentType.WOUND_CARE, VALID_DATE);
        TreatmentResponse response = new TreatmentResponse(10L, ANIMAL_CODE, SPECIALIST_CODE,
                VALID_DATE, TreatmentType.WOUND_CARE, request.description());

        when(animalRepository.findByAnimalCode(ANIMAL_CODE)).thenReturn(Optional.of(animal));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE)).thenReturn(Optional.of(specialist));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> {
            Treatment treatment = invocation.getArgument(0);
            treatment.setId(10L);
            return treatment;
        });
        when(mapper.toResponse(any(Treatment.class))).thenReturn(response);

        TreatmentResponse result = service.register(request);

        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Treatment> captor = ArgumentCaptor.forClass(Treatment.class);
        verify(treatmentRepository).save(captor.capture());
        Treatment saved = captor.getValue();
        assertThat(saved.getAnimal()).isSameAs(animal);
        assertThat(saved.getSpecialist()).isSameAs(specialist);
        assertThat(saved.getPerformedAt()).isEqualTo(VALID_DATE);
        assertThat(saved.getType()).isEqualTo(TreatmentType.WOUND_CARE);
        assertThat(saved.getDescription()).isEqualTo(request.description());
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        when(animalRepository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-999", SPECIALIST_CODE, VALID_DATE, TreatmentType.WOUND_CARE, "Cleaning");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verifyNoInteractions(specialistRepository, treatmentRepository, mapper);
    }

    @Test
    void shouldThrowResourceNotFoundWhenSpecialistDoesNotExist() {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode("SPEC-999")).thenReturn(Optional.empty());

        CreateTreatmentRequest request = new CreateTreatmentRequest(
                ANIMAL_CODE, "SPEC-999", VALID_DATE, TreatmentType.WOUND_CARE, "Cleaning");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SPEC-999");

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 6
    @Test
    void shouldRejectInactiveSpecialistAndNeverSave() {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE))
                .thenReturn(Optional.of(specialist(false)));

        assertThatThrownBy(() -> service.register(request(TreatmentType.WOUND_CARE, VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(SPECIALIST_CODE);

        verify(treatmentRepository, never()).save(any());
    }

    // TEST 7
    @Test
    void shouldRejectTreatmentWhenRescueCaseIsReleased() {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(RescueStatus.RELEASED)));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(TreatmentType.OBSERVATION, VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RELEASED");

        verify(treatmentRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"ADMITTED", "READY_FOR_RELEASE", "RELEASED", "CLOSED"})
    void shouldRejectTreatmentWhenStatusDoesNotAllowIt(RescueStatus status) {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(status)));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE))
                .thenReturn(Optional.of(specialist(true)));

        assertThatThrownBy(() -> service.register(request(TreatmentType.OBSERVATION, VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldRejectTreatmentBeforeRescueDate() {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(RescueStatus.IN_REHABILITATION)));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE))
                .thenReturn(Optional.of(specialist(true)));

        LocalDateTime beforeRescue = LocalDateTime.of(2026, 8, 15, 9, 0);

        assertThatThrownBy(() -> service.register(request(TreatmentType.WOUND_CARE, beforeRescue)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2026-08-20");

        verify(treatmentRepository, never()).save(any());
    }

    @Test
    void shouldAcceptTreatmentOnTheRescueDay() {
        when(animalRepository.findByAnimalCode(ANIMAL_CODE))
                .thenReturn(Optional.of(animalWithCase(RescueStatus.UNDER_EVALUATION)));
        when(specialistRepository.findByProfessionalCode(SPECIALIST_CODE))
                .thenReturn(Optional.of(specialist(true)));
        when(treatmentRepository.save(any(Treatment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Límite: misma fecha del rescate, a medianoche
        service.register(request(TreatmentType.OBSERVATION, RESCUE_DATE.atStartOfDay()));

        verify(treatmentRepository).save(any(Treatment.class));
    }

    @Test
    void shouldFindTreatmentsByAnimalCode() {
        Animal animal = animalWithCase(RescueStatus.IN_REHABILITATION);
        Treatment treatment = new Treatment(animal, specialist(true), VALID_DATE,
                TreatmentType.HYDRATION, "Oral hydration");
        TreatmentResponse response = new TreatmentResponse(1L, ANIMAL_CODE, SPECIALIST_CODE,
                VALID_DATE, TreatmentType.HYDRATION, "Oral hydration");

        when(treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc(ANIMAL_CODE))
                .thenReturn(List.of(treatment));
        when(mapper.toResponse(treatment)).thenReturn(response);

        assertThat(service.findByAnimalCode(ANIMAL_CODE)).containsExactly(response);
    }

    // ---------- fixtures ----------

    private static Animal animalWithCase(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-2026-100", RESCUE_DATE, "Bahía de Taganga", status);
        Animal animal = new Animal(ANIMAL_CODE, "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private static Specialist specialist(boolean active) {
        return new Specialist(SPECIALIST_CODE, "Elena", "Vargas", "elena.vargas@deepblue.org", active);
    }

    private static CreateTreatmentRequest request(TreatmentType type, LocalDateTime performedAt) {
        return new CreateTreatmentRequest(ANIMAL_CODE, SPECIALIST_CODE, performedAt, type,
                "Cleaning of left front flipper injury.");
    }
}
