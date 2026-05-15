package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ProductDto {

    public record CreateProductRequest(
            @NotBlank(message = "Product name is required")
            String name,
            String description,
            @NotNull(message = "Category is required")
            Long categoryId,
            String unit,
            @NotNull(message = "Price is required")
            @Positive(message = "Price must be positive")
            BigDecimal pricePerUnit,
            String imageUrl,
            BigDecimal minOrderQty
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            String description,
            Long categoryId,
            String categoryName,
            String unit,
            BigDecimal pricePerUnit,
            String imageUrl,
            Boolean available,
            BigDecimal minOrderQty
    ) {}
}
