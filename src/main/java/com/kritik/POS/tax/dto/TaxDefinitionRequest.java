package com.kritik.POS.tax.dto;

import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxDefinitionRequest(
        Long id,
        @NotNull(message = "restaurantId is required") Long restaurantId,
        @NotBlank(message = "code is required") String code,
        @NotBlank(message = "displayName is required") String displayName,
        @NotNull(message = "kind is required") TaxDefinitionKind kind,
        @NotNull(message = "valueType is required") TaxValueType valueType,
        @NotNull(message = "defaultValue is required") BigDecimal defaultValue,
        String currencyCode,
        Boolean isRecoverable,
        Boolean isActive
) {
}
