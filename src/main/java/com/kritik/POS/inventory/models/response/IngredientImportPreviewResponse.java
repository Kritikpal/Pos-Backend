package com.kritik.POS.inventory.models.response;

import java.time.LocalDateTime;
import java.util.List;

public record IngredientImportPreviewResponse(
        String previewToken,
        LocalDateTime expiresAt,
        Integer totalRowsRead,
        Integer validRowCount,
        Integer invalidRowCount,
        Integer rowsToCreate,
        Integer rowsToUpdate,
        List<String> normalizedHeaders,
        List<IngredientImportRowResult> rows
) {
}
