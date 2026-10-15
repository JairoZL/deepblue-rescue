package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.dto.response.SpecialistResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.SpecialistMapper;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.service.impl.SpecialistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpecialistServiceImplTest {

    private static final String SPECIALIST_CODE = "SPEC-001";

    @Mock
    private SpecialistRepository repository;

    @Mock
    private SpecialistMapper mapper;

    @InjectMocks
    private SpecialistServiceImpl service;

    @Test
    void shouldFindSpecialistByCode() {
        Specialist specialist = specialist(SPECIALIST_CODE);
        SpecialistResponse response = response(SPECIALIST_CODE, "Marine Biology", "Veterinary Medicine");

        when(repository.findByProfessionalCode(SPECIALIST_CODE)).thenReturn(Optional.of(specialist));
        when(mapper.toResponse(specialist)).thenReturn(response);

        SpecialistResponse result = service.findByCode(SPECIALIST_CODE);

        assertThat(result).isEqualTo(response);
        assertThat(result.expertiseAreas()).containsExactlyInAnyOrder("Veterinary Medicine", "Marine Biology");
    }

    @Test
    void shouldThrowResourceNotFoundWhenSpecialistDoesNotExist() {
        when(repository.findByProfessionalCode("SPEC-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("SPEC-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("SPEC-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindAllSpecialists() {
        Specialist first = specialist("SPEC-001");
        Specialist second = specialist("SPEC-002");
        SpecialistResponse firstResponse = response("SPEC-001", "Marine Biology");
        SpecialistResponse secondResponse = response("SPEC-002", "Veterinary Medicine");

        when(repository.findAll()).thenReturn(List.of(first, second));
        when(mapper.toResponse(first)).thenReturn(firstResponse);
        when(mapper.toResponse(second)).thenReturn(secondResponse);

        assertThat(service.findAllSpecialists()).containsExactlyInAnyOrder(firstResponse, secondResponse);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoSpecialists() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAllSpecialists()).isEmpty();

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindActiveSpecialistsByExpertise() {
        Specialist specialist = specialist(SPECIALIST_CODE);
        SpecialistResponse response = response(SPECIALIST_CODE, "Marine Biology");

        when(repository.findActiveByExpertise("Marine Biology")).thenReturn(List.of(specialist));
        when(mapper.toResponse(specialist)).thenReturn(response);

        assertThat(service.findActiveByExpertise("Marine Biology")).containsExactly(response);
        verify(repository).findActiveByExpertise("Marine Biology");
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveSpecialistMatchesExpertise() {
        when(repository.findActiveByExpertise("Ornithology")).thenReturn(List.of());

        assertThat(service.findActiveByExpertise("Ornithology")).isEmpty();

        verify(mapper, never()).toResponse(any());
    }

    // ---------- fixtures ----------

    private static Specialist specialist(String code) {
        return new Specialist(code, "Elena", "Vargas", "elena.vargas@deepblue.org", true);
    }

    private static SpecialistResponse response(String code, String... expertiseAreas) {
        return new SpecialistResponse(1L, code, "Elena", "Vargas", "elena.vargas@deepblue.org",
                true, Set.of(expertiseAreas));
    }
}
