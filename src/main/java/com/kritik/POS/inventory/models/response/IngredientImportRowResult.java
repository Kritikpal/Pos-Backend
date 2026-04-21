package com.kritik.POS.inventory.models.response;

import java.util.List;
import java.util.Map;

public record IngredientImportRowResult(
        Integer rowNumber,
        String action,
        String sku,
        Map<String, String> normalizedValues,
        List<String> errors
) {
}
