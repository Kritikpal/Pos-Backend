package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.models.response.IngredientImportRowResult;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IngredientImportPreviewStore {

    private final Map<String, PreviewSession> sessions = new ConcurrentHashMap<>();

    public void save(PreviewSession session) {
        purgeExpired();
        sessions.put(session.token(), session);
    }

    public PreviewSession get(String token) {
        purgeExpired();
        return sessions.get(token);
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    private void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record PreviewSession(String token, Long userId, Long restaurantId, boolean overwriteNulls, String checksum,
                                 LocalDateTime expiresAt, List<String> normalizedHeaders,
                                 List<ParsedIngredientRow> validRows, List<IngredientImportRowResult> rowResults,
                                 int invalidRowCount) {
        public PreviewSession(String token,
                              Long userId,
                              Long restaurantId,
                              boolean overwriteNulls,
                              String checksum,
                              LocalDateTime expiresAt,
                              List<String> normalizedHeaders,
                              List<ParsedIngredientRow> validRows,
                              List<IngredientImportRowResult> rowResults,
                              int invalidRowCount) {
            this.token = token;
            this.userId = userId;
            this.restaurantId = restaurantId;
            this.overwriteNulls = overwriteNulls;
            this.checksum = checksum;
            this.expiresAt = expiresAt;
            this.normalizedHeaders = List.copyOf(normalizedHeaders);
            this.validRows = List.copyOf(validRows);
            this.rowResults = List.copyOf(rowResults);
            this.invalidRowCount = invalidRowCount;
        }
    }

    @Getter
    public static class ParsedIngredientRow {
        private final int rowNumber;
        private final String action;
        private final String sku;
        private final String ingredientName;
        private final String description;
        private final String category;
        private final Long restaurantIdInFile;
        private final Double totalStock;
        private final String unitCode;
        private final Double reorderLevel;
        private final Long supplierId;
        private final String supplierName;
        private final Boolean isActive;
        private final Map<String, String> normalizedValues;

        public ParsedIngredientRow(int rowNumber,
                                   String action,
                                   String sku,
                                   String ingredientName,
                                   String description,
                                   String category,
                                   Long restaurantIdInFile,
                                   Double totalStock,
                                   String unitCode,
                                   Double reorderLevel,
                                   Long supplierId,
                                   String supplierName,
                                   Boolean isActive,
                                   Map<String, String> normalizedValues) {
            this.rowNumber = rowNumber;
            this.action = action;
            this.sku = sku;
            this.ingredientName = ingredientName;
            this.description = description;
            this.category = category;
            this.restaurantIdInFile = restaurantIdInFile;
            this.totalStock = totalStock;
            this.unitCode = unitCode;
            this.reorderLevel = reorderLevel;
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.isActive = isActive;
            this.normalizedValues = Map.copyOf(normalizedValues);
        }
    }
}
