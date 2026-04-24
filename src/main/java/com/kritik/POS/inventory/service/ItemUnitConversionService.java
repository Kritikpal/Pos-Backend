package com.kritik.POS.inventory.service;

import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.models.request.UnitConversionRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ItemUnitConversionService {

    UnitMaster resolveBaseUnit(Long baseUnitId, String legacyUnitCode, String fallbackUnitCode);

    List<ItemUnitConversion> saveItemWithConversions(Long restaurantId,
                                                     UnitConversionSourceType sourceType,
                                                     String sourceId,
                                                     UnitMaster baseUnit,
                                                     List<UnitConversionRequest> conversions);

    List<ItemUnitConversion> updateConversionsForExistingItem(Long restaurantId,
                                                              UnitConversionSourceType sourceType,
                                                              String sourceId,
                                                              UnitMaster baseUnit,
                                                              List<UnitConversionRequest> conversions);

    BigDecimal convertToBase(Long restaurantId,
                             UnitConversionSourceType sourceType,
                             String sourceId,
                             Long unitId,
                             BigDecimal qty);

    List<ItemUnitConversion> getConversions(Long restaurantId,
                                            UnitConversionSourceType sourceType,
                                            String sourceId);
}
