package com.deepblue.rescue.dto.request;

import com.deepblue.rescue.domain.TreatmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTreatmentRequest(

        @NotBlank
        @Size(max = 20)
        String animalCode,

        @NotBlank
        @Size(max = 20)
        String specialistCode,

        @NotNull
        LocalDateTime performedAt,

        @NotNull
        TreatmentType type,

        @NotBlank
        String description

) {
}
