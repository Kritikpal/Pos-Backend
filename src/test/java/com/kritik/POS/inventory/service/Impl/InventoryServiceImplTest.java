package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuItemIngredientRepository menuItemIngredientRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private InventoryUtil inventoryUtil;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void deductStockForOrderUsesBulkQueriesAndRefreshesAffectedMenus() {
        Order order = new Order();
        order.setOrderId("order-1");

        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(directDeduction("SKU-1", 3L)));
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(ingredientDeduction("ING-1", 2.5)));
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-1")).thenReturn(List.of(11L, 12L));
        when(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(anyCollection())).thenReturn(List.of(21L, 22L));
        when(stockRepository.deductStockQuantityIfAvailable(anyString(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(ingredientStockRepository.deductStockQuantityIfAvailable(anyString(), anyDouble(), any(LocalDateTime.class))).thenReturn(1);

        inventoryService.deductStockForOrder(order);

        verify(stockRepository).deductStockQuantityIfAvailable(anyString(), anyInt(), any(LocalDateTime.class));
        verify(ingredientStockRepository).deductStockQuantityIfAvailable(anyString(), anyDouble(), any(LocalDateTime.class));
        verify(menuItemRepository).markUnavailableByIds(List.of(11L, 12L));
        verify(menuItemRepository).markDirectMenusAvailableByIds(List.of(11L, 12L));
        verify(menuItemRepository).refreshRecipeAvailabilityByIds(List.of(21L, 22L));
    }

    @Test
    void restoreStockForRefundAddsBackInventoryAndRefreshesMenus() {
        Order order = new Order();
        order.setOrderId("order-2");

        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-2"))
                .thenReturn(List.of(directDeduction("SKU-2", 2L)));
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-2"))
                .thenReturn(List.of(ingredientDeduction("ING-2", 1.5)));
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-2")).thenReturn(List.of(31L));
        when(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(anyCollection())).thenReturn(List.of(41L));
        when(stockRepository.increaseStockQuantity(anyString(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(ingredientStockRepository.increaseStockQuantity(anyString(), anyDouble(), any(LocalDateTime.class))).thenReturn(1);

        inventoryService.restoreStockForRefund(order);

        verify(stockRepository).increaseStockQuantity(anyString(), anyInt(), any(LocalDateTime.class));
        verify(ingredientStockRepository).increaseStockQuantity(anyString(), anyDouble(), any(LocalDateTime.class));
        verify(menuItemRepository).markUnavailableByIds(List.of(31L));
        verify(menuItemRepository).markDirectMenusAvailableByIds(List.of(31L));
        verify(menuItemRepository).refreshRecipeAvailabilityByIds(List.of(41L));
    }

    @Test
    void deductStockForOrderRequiresAnOrderId() {
        Order order = new Order();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> inventoryService.deductStockForOrder(order))
                .isInstanceOf(RuntimeException.class);

        verify(saleItemRepository, never()).findDirectStockDeductionsByOrderId(anyString());
    }

    private DirectStockDeductionProjection directDeduction(String sku, Long quantity) {
        return new DirectStockDeductionProjection() {
            @Override
            public String getSku() {
                return sku;
            }

            @Override
            public Long getQuantity() {
                return quantity;
            }
        };
    }

    private IngredientStockDeductionProjection ingredientDeduction(String sku, Double quantity) {
        return new IngredientStockDeductionProjection() {
            @Override
            public String getSku() {
                return sku;
            }

            @Override
            public Double getQuantity() {
                return quantity;
            }
        };
    }
}
