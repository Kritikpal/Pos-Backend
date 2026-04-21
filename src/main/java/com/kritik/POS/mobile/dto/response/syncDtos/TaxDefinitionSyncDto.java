package com.kritik.POS.mobile.dto.response.syncDtos;

import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxDefinitionSyncDto(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
