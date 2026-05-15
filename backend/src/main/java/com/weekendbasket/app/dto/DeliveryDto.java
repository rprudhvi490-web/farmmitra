package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class DeliveryDto {

    public record CreateBatchRequest(
            @NotBlank(message = "Batch label is required")
            String batchLabel,
            @NotNull(message = "Cycle ID is required")
            Long cycleId,
            LocalDate deliveryDate,
            Long assignedToUserId,
            @NotEmpty(message = "At least one order is required")
            List<Long> orderIds
    ) {}

    public record BatchOrderSummary(
            Long orderId,
            String orderNumber,
            String customerName,
            String flatNumber,
            String block,
            String deliverySlot,
            String orderStatus
    ) {}

    public record BatchResponse(
            Long id,
            String batchLabel,
            Long cycleId,
            String cycleLabel,
            LocalDate deliveryDate,
            Long assignedToUserId,
            String assignedToName,
            String status,
            List<BatchOrderSummary> orders
    ) {}
}
