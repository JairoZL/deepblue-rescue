package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.response.AnimalResponse;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public interface AnimalService {

    AnimalResponse findByCode(@NotBlank String animalCode);

    List<AnimalResponse> findAnimalsInRehabilitation();

    boolean canReceiveTreatment(@NotBlank String animalCode);
}
