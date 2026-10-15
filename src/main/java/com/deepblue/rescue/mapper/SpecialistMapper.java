package com.deepblue.rescue.mapper;

import com.deepblue.rescue.domain.Expertise;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.dto.response.SpecialistResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpecialistMapper {

    SpecialistResponse toResponse(Specialist specialist);

    default String toExpertiseName(Expertise expertise) {
        return expertise.getName();
    }
}
