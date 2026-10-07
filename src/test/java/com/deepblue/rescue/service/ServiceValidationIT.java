package com.deepblue.rescue.service;

import com.deepblue.rescue.TestcontainersConfiguration;
import com.deepblue.rescue.domain.TreatmentType;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que la validación de métodos (@Validated + restricciones en las interfaces)
 * se aplica sobre los beans reales de Spring. Los unit tests con Mockito no pueden
 * probarlo porque instancian los servicios sin el proxy de validación.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ServiceValidationIT {

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private RescueCaseService rescueCaseService;

    @Autowired
    private AnimalService animalService;

    @Test
    void shouldRejectTreatmentRequestWithEveryFieldInvalid() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(" ", "", null, null, "");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOfSatisfying(ConstraintViolationException.class, ex ->
                        assertThat(violatedFields(ex)).containsExactlyInAnyOrder(
                                "animalCode", "specialistCode", "performedAt", "type", "description"));
    }

    @Test
    void shouldRejectAnimalCodeLongerThanColumn() {
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-" + "9".repeat(18), "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOfSatisfying(ConstraintViolationException.class, ex ->
                        assertThat(violatedFields(ex)).containsExactly("animalCode"));
    }

    @Test
    void shouldRejectNullTreatmentRequest() {
        assertThatThrownBy(() -> treatmentService.register(null))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldRejectStatusChangeWithoutStatus() {
        assertThatThrownBy(() -> rescueCaseService.changeStatus("RES-001", new ChangeRescueStatusRequest(null)))
                .isInstanceOfSatisfying(ConstraintViolationException.class, ex ->
                        assertThat(violatedFields(ex)).containsExactly("status"));
    }

    @Test
    void shouldRejectBlankCodes() {
        assertThatThrownBy(() -> rescueCaseService.findByCode(" "))
                .isInstanceOf(ConstraintViolationException.class);

        assertThatThrownBy(() -> animalService.canReceiveTreatment(""))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void shouldLetValidRequestReachBusinessRules() {
        // Una solicitud válida pasa la validación y llega a la Regla 1:
        // el animal no existe en la base vacía.
        CreateTreatmentRequest request = new CreateTreatmentRequest(
                "AN-404", "SPEC-001", LocalDateTime.of(2026, 8, 21, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper injury.");

        assertThatThrownBy(() -> treatmentService.register(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("AN-404");
    }

    /** Último nodo de cada propertyPath: el campo del record que violó la restricción. */
    private static Set<String> violatedFields(ConstraintViolationException ex) {
        Set<String> fields = new HashSet<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String leaf = null;
            for (Path.Node node : violation.getPropertyPath()) {
                leaf = node.getName();
            }
            fields.add(leaf);
        }
        return fields;
    }
}
