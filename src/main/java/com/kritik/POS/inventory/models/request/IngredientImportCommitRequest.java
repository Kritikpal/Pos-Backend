package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.NotBlank;

public record IngredientImportCommitRequest(
        @NotBlank(message = "Preview token is required")
        String previewToken
) {
}
