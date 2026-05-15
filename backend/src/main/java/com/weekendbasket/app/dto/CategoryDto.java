package com.weekendbasket.app.dto;

import jakarta.validation.constraints.NotBlank;

public class CategoryDto {

    public record CreateCategoryRequest(
            @NotBlank(message = "Category name is required")
            String name,
            String imageUrl,
            Integer displayOrder
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String imageUrl,
            Integer displayOrder,
            Boolean active
    ) {}
}
