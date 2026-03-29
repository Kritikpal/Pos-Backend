package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        Long categoryId,
        Long restaurantId,
        @NotBlank(message = "Category name is required")
        String categoryName,
        @NotBlank(message = "Category Description is required")
        String categoryDescription
) {



}
