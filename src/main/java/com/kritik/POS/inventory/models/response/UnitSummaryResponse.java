package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.entity.unit.UnitMaster;

public record UnitSummaryResponse(
        Long id,
        String code,
        String displayName,
        Boolean active
) {
    public static UnitSummaryResponse fromEntity(UnitMaster unitMaster) {
        if (unitMaster == null) {
            return null;
        }
        return new UnitSummaryResponse(
                unitMaster.getId(),
                unitMaster.getCode(),
                unitMaster.getDisplayName(),
                unitMaster.getActive()
        );
    }
}
