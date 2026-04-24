package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.models.request.UnitConversionRequest;
import com.kritik.POS.inventory.repository.ItemUnitConversionRepository;
import com.kritik.POS.inventory.repository.UnitMasterRepository;
import com.kritik.POS.inventory.service.ItemUnitConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ItemUnitConversionServiceImpl implements ItemUnitConversionService {

    private final ItemUnitConversionRepository itemUnitConversionRepository;
    private final UnitMasterRepository unitMasterRepository;

    public ItemUnitConversionServiceImpl(ItemUnitConversionRepository itemUnitConversionRepository,
                                         UnitMasterRepository unitMasterRepository) {
        this.itemUnitConversionRepository = itemUnitConversionRepository;
        this.unitMasterRepository = unitMasterRepository;
    }

    @Override
    @Transactional
    public UnitMaster resolveBaseUnit(Long baseUnitId, String legacyUnitCode, String fallbackUnitCode) {
        if (baseUnitId != null) {
            return unitMasterRepository.findById(baseUnitId)
                    .orElseThrow(() -> new AppException("Base unit not found", HttpStatus.BAD_REQUEST));
        }
        String resolvedCode = normalizeUnitCode(legacyUnitCode != null ? legacyUnitCode : fallbackUnitCode);
        return unitMasterRepository.findByCodeIgnoreCase(resolvedCode)
                .orElseGet(() -> {
                    UnitMaster unitMaster = new UnitMaster();
                    unitMaster.setCode(resolvedCode);
                    unitMaster.setDisplayName(resolvedCode);
                    unitMaster.setActive(true);
                    return unitMasterRepository.save(unitMaster);
                });
    }

    @Override
    @Transactional
    public List<ItemUnitConversion> saveItemWithConversions(Long restaurantId,
                                                            UnitConversionSourceType sourceType,
                                                            String sourceId,
                                                            UnitMaster baseUnit,
                                                            List<UnitConversionRequest> conversions) {
        return replaceConversions(restaurantId, sourceType, sourceId, baseUnit, conversions);
    }

    @Override
    @Transactional
    public List<ItemUnitConversion> updateConversionsForExistingItem(Long restaurantId,
                                                                     UnitConversionSourceType sourceType,
                                                                     String sourceId,
                                                                     UnitMaster baseUnit,
                                                                     List<UnitConversionRequest> conversions) {
        return replaceConversions(restaurantId, sourceType, sourceId, baseUnit, conversions);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal convertToBase(Long restaurantId,
                                    UnitConversionSourceType sourceType,
                                    String sourceId,
                                    Long unitId,
                                    BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Quantity must be greater than 0", HttpStatus.BAD_REQUEST);
        }
        if (unitId == null) {
            throw new AppException("Unit id is required", HttpStatus.BAD_REQUEST);
        }
        ItemUnitConversion conversion = itemUnitConversionRepository
                .findByRestaurantIdAndSourceTypeAndSourceIdAndUnitIdAndActiveTrue(restaurantId, sourceType, sourceId, unitId)
                .orElseThrow(() -> new AppException("Active unit conversion not found", HttpStatus.BAD_REQUEST));
        return qty.multiply(conversion.getFactorToBase());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemUnitConversion> getConversions(Long restaurantId,
                                                   UnitConversionSourceType sourceType,
                                                   String sourceId) {
        return itemUnitConversionRepository.findAllForSource(
                restaurantId,
                sourceType,
                sourceId
        );
    }

    private List<ItemUnitConversion> replaceConversions(Long restaurantId,
                                                        UnitConversionSourceType sourceType,
                                                        String sourceId,
                                                        UnitMaster baseUnit,
                                                        List<UnitConversionRequest> conversions) {
        List<UnitConversionRequest> normalizedRequests = normalizeRequests(baseUnit, conversions);
        validateDuplicateUnits(normalizedRequests);
        validateBaseUnitConversion(baseUnit, normalizedRequests);

        List<ItemUnitConversion> existing = itemUnitConversionRepository.findAllByRestaurantIdAndSourceTypeAndSourceId(
                restaurantId,
                sourceType,
                sourceId
        );
        if (!existing.isEmpty()) {
            itemUnitConversionRepository.deleteAll(existing);
            itemUnitConversionRepository.flush();
        }

        List<ItemUnitConversion> saved = new ArrayList<>();
        for (UnitConversionRequest request : normalizedRequests) {
            ItemUnitConversion conversion = new ItemUnitConversion();
            conversion.setRestaurantId(restaurantId);
            conversion.setSourceType(sourceType);
            conversion.setSourceId(sourceId);
            conversion.setUnit(unitMasterRepository.findById(request.unitId())
                    .orElseThrow(() -> new AppException("Unit not found", HttpStatus.BAD_REQUEST)));
            conversion.setFactorToBase(request.factorToBase());
            conversion.setPurchaseAllowed(request.purchaseAllowed() != null ? request.purchaseAllowed() : Boolean.TRUE);
            conversion.setSaleAllowed(request.saleAllowed() != null ? request.saleAllowed() : Boolean.FALSE);
            conversion.setActive(request.active() != null ? request.active() : Boolean.TRUE);
            saved.add(itemUnitConversionRepository.save(conversion));
        }

        saved.sort(Comparator.comparing(conversion -> conversion.getUnit().getCode(), String.CASE_INSENSITIVE_ORDER));
        return saved;
    }

    private List<UnitConversionRequest> normalizeRequests(UnitMaster baseUnit, List<UnitConversionRequest> conversions) {
        List<UnitConversionRequest> requests = new ArrayList<>();
        if (conversions != null) {
            requests.addAll(conversions);
        }
        boolean hasBaseUnit = requests.stream().anyMatch(request -> baseUnit.getId().equals(request.unitId()));
        if (!hasBaseUnit) {
            requests.add(new UnitConversionRequest(baseUnit.getId(), BigDecimal.ONE, true, true, true));
        }
        return requests;
    }

    private void validateDuplicateUnits(List<UnitConversionRequest> conversions) {
        Set<Long> unitIds = new HashSet<>();
        for (UnitConversionRequest request : conversions) {
            if (!unitIds.add(request.unitId())) {
                throw new AppException("Duplicate conversion is not allowed for the same unit", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateBaseUnitConversion(UnitMaster baseUnit, List<UnitConversionRequest> conversions) {
        UnitConversionRequest baseRequest = conversions.stream()
                .filter(request -> baseUnit.getId().equals(request.unitId()))
                .findFirst()
                .orElseThrow(() -> new AppException("Base unit conversion row is required", HttpStatus.BAD_REQUEST));
        if (baseRequest.factorToBase() == null || baseRequest.factorToBase().compareTo(BigDecimal.ONE) != 0) {
            throw new AppException("Base unit conversion must have factor 1", HttpStatus.BAD_REQUEST);
        }
        if (Boolean.FALSE.equals(baseRequest.active())) {
            throw new AppException("Base unit conversion must be active", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeUnitCode(String value) {
        String resolved = value == null ? "UNIT" : value.trim();
        if (resolved.isEmpty()) {
            resolved = "UNIT";
        }
        return resolved.toUpperCase(Locale.ROOT);
    }
}
