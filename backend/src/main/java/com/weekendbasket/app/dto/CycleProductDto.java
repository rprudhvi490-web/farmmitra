package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public class CycleProductDto {

    public record SetStockRequest(
            @NotNull Long productId,
            @NotNull @Positive BigDecimal maxStock
    ) {}

    public record BulkSetStockRequest(
            @NotNull List<SetStockRequest> items
    ) {}

    public record CycleProductResponse(
            Long id,
            Long productId,
            String productName,
            String unit,
            BigDecimal maxStock,
            BigDecimal orderedQty,
            BigDecimal remainingQty,
            Boolean soldOut
    ) {}

    // Used for auto-suggest: last cycle's max_stock for each product
    public record StockSuggestion(
            Long productId,
            String productName,
            String unit,
            BigDecimal suggestedMaxStock   // null if no previous cycle data
    ) {}
}
