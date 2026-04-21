package com.kritik.POS.tax.dto;

import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaxDefinitionResponseDto(
        Long id,
        Long restaurantId,
        String code,
        String displayName,
        TaxDefinitionKind kind,
        TaxValueType valueType,
        BigDecimal defaultValue,
        String currencyCode,
        boolean isRecoverable,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
