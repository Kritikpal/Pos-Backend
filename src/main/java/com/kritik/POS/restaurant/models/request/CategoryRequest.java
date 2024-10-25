package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.NotEmpty;

public record CategoryRequest(
        Long categoryId,
        @NotEmpty(message = "Category name is required")
        String categoryName,
        @NotEmpty(message = "Category Description is required")
        String categoryDescription
) {



}
