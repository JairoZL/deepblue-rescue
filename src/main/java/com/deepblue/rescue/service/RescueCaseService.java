package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public interface RescueCaseService {

    RescueCaseResponse findByCode(@NotBlank String caseCode);

    List<RescueCaseResponse> findByStatus(@NotNull RescueStatus status);

    RescueCaseResponse changeStatus(@NotBlank String caseCode,
                                    @NotNull @Valid ChangeRescueStatusRequest request);
}
