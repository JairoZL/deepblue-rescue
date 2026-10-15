package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.response.SpecialistResponse;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface SpecialistService {

    SpecialistResponse findByCode(@NotBlank String professionalCode);

    List<SpecialistResponse> findAllSpecialists();

    List<SpecialistResponse> findActiveByExpertise(@NotBlank String expertiseName);
}
