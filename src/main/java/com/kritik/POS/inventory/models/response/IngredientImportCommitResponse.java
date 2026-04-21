package com.kritik.POS.inventory.models.response;

import java.util.List;

public record IngredientImportCommitResponse(
        Integer createdCount,
        Integer updatedCount,
        Integer skippedOrErrorCount,
        Long elapsedTimeMs,
        Boolean availabilityRefreshTriggered,
        List<IngredientImportRowResult> rows
) {
}
