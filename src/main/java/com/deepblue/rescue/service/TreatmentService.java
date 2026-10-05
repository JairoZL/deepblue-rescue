package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface TreatmentService {

    TreatmentResponse register(@NotNull @Valid CreateTreatmentRequest request);

    List<TreatmentResponse> findByAnimalCode(@NotBlank String animalCode);
}
