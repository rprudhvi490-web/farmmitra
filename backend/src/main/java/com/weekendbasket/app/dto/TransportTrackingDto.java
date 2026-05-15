package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class TransportTrackingDto {

    public record AddStageRequest(
            @NotBlank(message = "Stage is required")
            String stage,
            String notes
    ) {}

    public record TransportStageResponse(
            Long id,
            Long cycleId,
            String stage,
            String notes,
            String updatedBy,
            LocalDateTime createdOn
    ) {}
}
