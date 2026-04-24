package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.models.request.UnitConversionRequest;
import com.kritik.POS.inventory.repository.ItemUnitConversionRepository;
import com.kritik.POS.inventory.repository.UnitMasterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemUnitConversionServiceImplTest {

    @Mock
    private ItemUnitConversionRepository itemUnitConversionRepository;

    @Mock
    private UnitMasterRepository unitMasterRepository;

    @InjectMocks
    private ItemUnitConversionServiceImpl itemUnitConversionService;

    @Test
    void convertToBaseUsesConfiguredFactor() {
        UnitMaster carton = unit(2L, "CARTON");
        ItemUnitConversion conversion = new ItemUnitConversion();
        conversion.setUnit(carton);
        conversion.setFactorToBase(new BigDecimal("30"));
        conversion.setActive(true);

        when(itemUnitConversionRepository.findByRestaurantIdAndSourceTypeAndSourceIdAndUnitIdAndActiveTrue(
                10L, UnitConversionSourceType.INGREDIENT, "ING-1", 2L
        )).thenReturn(Optional.of(conversion));

        BigDecimal converted = itemUnitConversionService.convertToBase(
                10L,
                UnitConversionSourceType.INGREDIENT,
                "ING-1",
                2L,
                new BigDecimal("2")
        );

        assertThat(converted).isEqualByComparingTo("60");
    }

    @Test
    void updateConversionsAddsRequiredBaseUnitRowWithFactorOne() {
        UnitMaster pcs = unit(1L, "PCS");
        UnitMaster carton = unit(2L, "CARTON");

        when(unitMasterRepository.findById(1L)).thenReturn(Optional.of(pcs));
        when(unitMasterRepository.findById(2L)).thenReturn(Optional.of(carton));
        when(itemUnitConversionRepository.findAllByRestaurantIdAndSourceTypeAndSourceId(
                10L, UnitConversionSourceType.DIRECT_ITEM, "15"
        )).thenReturn(List.of());
        when(itemUnitConversionRepository.save(any(ItemUnitConversion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ItemUnitConversion> saved = itemUnitConversionService.updateConversionsForExistingItem(
                10L,
                UnitConversionSourceType.DIRECT_ITEM,
                "15",
                pcs,
                List.of(new UnitConversionRequest(2L, new BigDecimal("12"), true, false, true))
        );

        assertThat(saved).hasSize(2);
        assertThat(saved).anyMatch(conversion ->
                "PCS".equals(conversion.getUnit().getCode())
                        && BigDecimal.ONE.compareTo(conversion.getFactorToBase()) == 0);
    }

    @Test
    void updateConversionsRejectsDuplicateUnits() {
        UnitMaster pcs = unit(1L, "PCS");

        assertThatThrownBy(() -> itemUnitConversionService.updateConversionsForExistingItem(
                10L,
                UnitConversionSourceType.INGREDIENT,
                "ING-1",
                pcs,
                List.of(
                        new UnitConversionRequest(1L, BigDecimal.ONE, true, true, true),
                        new UnitConversionRequest(1L, BigDecimal.ONE, true, true, true)
                )
        )).isInstanceOf(AppException.class);
    }

    private UnitMaster unit(Long id, String code) {
        UnitMaster unitMaster = new UnitMaster();
        unitMaster.setId(id);
        unitMaster.setCode(code);
        unitMaster.setDisplayName(code);
        unitMaster.setActive(true);
        return unitMaster;
    }
}
