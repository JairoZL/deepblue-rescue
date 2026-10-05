package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.AnimalSex;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.response.AnimalResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.AnimalMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.service.impl.AnimalServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnimalServiceImplTest {

    private static final String ANIMAL_CODE = "AN-2026-100";

    @Mock
    private AnimalRepository repository;

    @Mock
    private AnimalMapper mapper;

    @InjectMocks
    private AnimalServiceImpl service;

    @Test
    void shouldFindAnimalByCode() {
        Animal animal = animalWithCase(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = response(RescueStatus.IN_REHABILITATION);

        when(repository.findByAnimalCode(ANIMAL_CODE)).thenReturn(Optional.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findByCode(ANIMAL_CODE)).isEqualTo(response);
    }

    @Test
    void shouldThrowResourceNotFoundWhenAnimalDoesNotExist() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAnimalsInRehabilitation() {
        Animal animal = animalWithCase(RescueStatus.IN_REHABILITATION);
        AnimalResponse response = response(RescueStatus.IN_REHABILITATION);

        when(repository.findByRescueCaseStatus(RescueStatus.IN_REHABILITATION)).thenReturn(List.of(animal));
        when(mapper.toResponse(animal)).thenReturn(response);

        assertThat(service.findAnimalsInRehabilitation()).containsExactly(response);
        verify(repository).findByRescueCaseStatus(RescueStatus.IN_REHABILITATION);
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldAllowTreatmentWhileUnderCare(RescueStatus status) {
        when(repository.findByAnimalCode(ANIMAL_CODE)).thenReturn(Optional.of(animalWithCase(status)));

        assertThat(service.canReceiveTreatment(ANIMAL_CODE)).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RescueStatus.class, mode = EnumSource.Mode.EXCLUDE,
            names = {"UNDER_EVALUATION", "IN_REHABILITATION"})
    void shouldNotAllowTreatmentInAnyOtherStatus(RescueStatus status) {
        when(repository.findByAnimalCode(ANIMAL_CODE)).thenReturn(Optional.of(animalWithCase(status)));

        assertThat(service.canReceiveTreatment(ANIMAL_CODE)).isFalse();
    }

    @Test
    void shouldThrowResourceNotFoundWhenCheckingMissingAnimal() {
        when(repository.findByAnimalCode("AN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.canReceiveTreatment("AN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- fixtures ----------

    private static Animal animalWithCase(RescueStatus status) {
        RescueCase rescueCase = new RescueCase("RES-2026-100", LocalDate.of(2026, 8, 20), "Bahía de Taganga", status);
        Animal animal = new Animal(ANIMAL_CODE, "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);
        return animal;
    }

    private static AnimalResponse response(RescueStatus status) {
        return new AnimalResponse(1L, ANIMAL_CODE, "Green Sea Turtle", "Chelonia mydas",
                AnimalSex.FEMALE, "RES-2026-100", status);
    }
}
