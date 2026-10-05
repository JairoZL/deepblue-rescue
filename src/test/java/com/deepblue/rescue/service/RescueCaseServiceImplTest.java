package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueCenter;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
class RescueCaseServiceImplTest {

    private static final String CASE_CODE = "RES-001";

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    // TEST 1
    @Test
    void shouldFindRescueCaseByCode() {
        // Arrange
        RescueCase rescueCase = rescueCase(RescueStatus.ADMITTED);
        RescueCaseResponse response = response(RescueStatus.ADMITTED);

        when(repository.findByCaseCode(CASE_CODE)).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        // Act
        RescueCaseResponse result = service.findByCode(CASE_CODE);

        // Assert
        assertThat(result).isEqualTo(response);
        verify(repository).findByCaseCode(CASE_CODE);
        verify(mapper).toResponse(rescueCase);
    }

    // TEST 2
    @Test
    void shouldThrowResourceNotFoundWhenRescueCaseDoesNotExist() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("RES-999");

        verify(mapper, never()).toResponse(any());
    }

    @Test
    void shouldFindRescueCasesByStatus() {
        RescueCase rescueCase = rescueCase(RescueStatus.IN_REHABILITATION);
        RescueCaseResponse response = response(RescueStatus.IN_REHABILITATION);

        when(repository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        List<RescueCaseResponse> result = service.findByStatus(RescueStatus.IN_REHABILITATION);

        assertThat(result).containsExactly(response);
    }

    // TEST 3
    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = rescueCase(RescueStatus.ADMITTED);
        RescueCaseResponse response = response(RescueStatus.UNDER_EVALUATION);

        when(repository.findByCaseCode(CASE_CODE)).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus(
                CASE_CODE, new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));

        assertThat(result).isEqualTo(response);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);
        verify(repository).save(rescueCase);
    }

    // TEST 4
    @Test
    void shouldRejectInvalidTransitionAndNeverSave() {
        RescueCase rescueCase = rescueCase(RescueStatus.ADMITTED);

        when(repository.findByCaseCode(CASE_CODE)).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                CASE_CODE, new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ADMITTED")
                .hasMessageContaining("READY_FOR_RELEASE");

        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.ADMITTED);
        verify(repository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @ParameterizedTest
    @CsvSource({
            "ADMITTED, UNDER_EVALUATION",
            "UNDER_EVALUATION, IN_REHABILITATION",
            "IN_REHABILITATION, READY_FOR_RELEASE",
            "READY_FOR_RELEASE, RELEASED"
    })
    void shouldAllowEveryStepOfTheRescueFlow(RescueStatus current, RescueStatus next) {
        RescueCase rescueCase = rescueCase(current);

        when(repository.findByCaseCode(CASE_CODE)).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);

        service.changeStatus(CASE_CODE, new ChangeRescueStatusRequest(next));

        assertThat(rescueCase.getStatus()).isEqualTo(next);
        verify(repository).save(rescueCase);
    }

    @ParameterizedTest
    @EnumSource(RescueStatus.class)
    void shouldRejectAnyTransitionFromReleased(RescueStatus next) {
        RescueCase rescueCase = rescueCase(RescueStatus.RELEASED);

        when(repository.findByCaseCode(CASE_CODE)).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(CASE_CODE, new ChangeRescueStatusRequest(next)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundWhenChangingStatusOfMissingCase() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatus(
                "RES-999", new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION)))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    // ---------- fixtures ----------

    private static RescueCase rescueCase(RescueStatus status) {
        RescueCenter center = new RescueCenter("CTR-001", "DeepBlue Caribe", "Santa Marta");
        RescueCase rescueCase = new RescueCase(CASE_CODE, LocalDate.of(2026, 8, 20), "Bahía de Taganga", status);
        rescueCase.setId(1L);
        center.addCase(rescueCase);
        return rescueCase;
    }

    private static RescueCaseResponse response(RescueStatus status) {
        return new RescueCaseResponse(1L, CASE_CODE, LocalDate.of(2026, 8, 20),
                "Bahía de Taganga", status, "CTR-001", "AN-001");
    }
}
