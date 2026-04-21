package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.models.request.IngredientRequest;
import com.kritik.POS.restaurant.models.response.IngredientResponse;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceImplTest {

    @Mock
    private InventoryUtil inventoryUtil;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @InjectMocks
    private IngredientServiceImpl ingredientService;

    @Test
    void saveIngredientCreatesMetadataOnlyIngredientWithZeroStock() {
        when(tenantAccessService.resolveAccessibleRestaurantId(10L)).thenReturn(10L);
        when(ingredientStockRepository.save(any(IngredientStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponse response = ingredientService.saveIngredient(new IngredientRequest(
                null,
                10L,
                "Paneer",
                "Fresh paneer",
                "Dairy",
                null,
                2.0,
                "KG",
                true
        ));

        assertThat(response.getTotalStock()).isEqualTo(0.0);
        assertThat(response.getReorderLevel()).isEqualTo(2.0);
        verify(inventoryUtil).syncMenuAvailabilityForIngredient(response.getSku());
    }

    @Test
    void saveIngredientPreservesExistingStockQuantity() {
        IngredientStock existing = new IngredientStock();
        existing.setSku("ING-1");
        existing.setRestaurantId(10L);
        existing.setIngredientName("Old Paneer");
        existing.setTotalStock(5.0);
        existing.setUnitOfMeasure("KG");
        existing.setIsActive(true);
        existing.setIsDeleted(false);

        when(inventoryUtil.getAccessibleIngredient("ING-1")).thenReturn(existing);
        when(tenantAccessService.resolveAccessibleRestaurantId(10L)).thenReturn(10L);
        when(ingredientStockRepository.save(any(IngredientStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IngredientResponse response = ingredientService.saveIngredient(new IngredientRequest(
                "ING-1",
                10L,
                "Updated Paneer",
                "Updated",
                "Dairy",
                null,
                3.0,
                "KG",
                false
        ));

        assertThat(response.getIngredientName()).isEqualTo("Updated Paneer");
        assertThat(response.getTotalStock()).isEqualTo(5.0);
        assertThat(response.getIsActive()).isFalse();
    }
}
