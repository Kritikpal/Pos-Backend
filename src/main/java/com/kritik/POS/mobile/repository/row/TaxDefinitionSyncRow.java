package com.kritik.POS.mobile.repository.row;

import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxDefinitionSyncRow(
        Long id,
        Long restaurantId,
        String code,
        String displayName,
        TaxDefinitionKind kind,
        TaxValueType valueType,
        BigDecimal defaultValue,
        String currencyCode,
        Boolean isRecoverable,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime syncTs
) implements TombstoneSyncRow {

    @Override
    public String cursorKey() {
        return String.valueOf(id);
    }
}
