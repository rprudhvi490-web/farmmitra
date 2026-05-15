package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class OrderDto {

    public record OrderItemRequest(
            @NotNull(message = "Product ID is required")
            Long productId,
            @NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be positive")
            BigDecimal quantity
    ) {}

    public record PlaceOrderRequest(
            @NotEmpty(message = "Order must have at least one item")
            List<OrderItemRequest> items,
            String notes
    ) {}

    public record OrderItemResponse(
            Long productId,
            String productName,
            String unit,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice
    ) {}

    public record OrderResponse(
            Long id,
            String orderNumber,
            Long cycleId,
            String cycleLabel,
            String status,
            String deliverySlot,
            BigDecimal totalAmount,
            BigDecimal referralDiscount,
            BigDecimal amountToCollect,
            String paymentMethod,
            String paymentStatus,
            String notes,
            List<OrderItemResponse> items
    ) {}

    public record UpdateOrderStatusRequest(
            @NotNull(message = "Status is required")
            String status
    ) {}
}
