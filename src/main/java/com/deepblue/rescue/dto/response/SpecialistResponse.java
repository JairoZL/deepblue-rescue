package com.deepblue.rescue.dto.response;

import java.util.Set;

public record SpecialistResponse(

        Long id,

        String professionalCode,

        String firstName,

        String lastName,

        String email,

        Boolean active,

        Set<String> expertiseAreas

) {
}
