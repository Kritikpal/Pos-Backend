package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.entity.MenuItem;
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
    private PreparedItemStockRepository preparedItemStockRepository;

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
        MenuItem directMenu = buildDirectMenu(11L, "SKU-1");
        MenuItem recipeMenu = buildRecipeMenu(21L);

        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(directDeduction("SKU-1", 3L)));
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(ingredientDeduction("ING-1", 2.5)));
        when(preparedItemStockRepository.findPreparedStockDeductionsByOrderId("order-1")).thenReturn(List.of());
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-1")).thenReturn(List.of(11L, 12L));
        when(saleItemRepository.findDistinctPreparedMenuIdsByOrderId("order-1")).thenReturn(List.of());
        when(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(anyCollection())).thenReturn(List.of(21L, 22L));
        when(stockRepository.deductStockQuantityIfAvailable(anyString(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(ingredientStockRepository.deductStockQuantityIfAvailable(anyString(), anyDouble(), any(LocalDateTime.class))).thenReturn(1);
        when(menuItemRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(directMenu, recipeMenu));

        inventoryService.deductStockForOrder(order);

        verify(stockRepository).deductStockQuantityIfAvailable(anyString(), anyInt(), any(LocalDateTime.class));
        verify(ingredientStockRepository).deductStockQuantityIfAvailable(anyString(), anyDouble(), any(LocalDateTime.class));
        verify(menuItemRepository).findAllByIdIn(anyCollection());
        verify(menuItemRepository).saveAll(anyCollection());
    }

    @Test
    void restoreStockForRefundAddsBackInventoryAndRefreshesMenus() {
        Order order = new Order();
        order.setOrderId("order-2");
        MenuItem directMenu = buildDirectMenu(31L, "SKU-2");
        MenuItem recipeMenu = buildRecipeMenu(41L);

        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-2"))
                .thenReturn(List.of(directDeduction("SKU-2", 2L)));
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-2"))
                .thenReturn(List.of(ingredientDeduction("ING-2", 1.5)));
        when(preparedItemStockRepository.findPreparedStockDeductionsByOrderId("order-2")).thenReturn(List.of());
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-2")).thenReturn(List.of(31L));
        when(saleItemRepository.findDistinctPreparedMenuIdsByOrderId("order-2")).thenReturn(List.of());
        when(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(anyCollection())).thenReturn(List.of(41L));
        when(stockRepository.increaseStockQuantity(anyString(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(ingredientStockRepository.increaseStockQuantity(anyString(), anyDouble(), any(LocalDateTime.class))).thenReturn(1);
        when(menuItemRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(directMenu, recipeMenu));

        inventoryService.restoreStockForRefund(order);

        verify(stockRepository).increaseStockQuantity(anyString(), anyInt(), any(LocalDateTime.class));
        verify(ingredientStockRepository).increaseStockQuantity(anyString(), anyDouble(), any(LocalDateTime.class));
        verify(menuItemRepository).findAllByIdIn(anyCollection());
        verify(menuItemRepository).saveAll(anyCollection());
    }

    @Test
    void deductStockForOrderUsesPreparedStockForPreparedMenus() {
        Order order = new Order();
        order.setOrderId("order-3");
        MenuItem preparedMenu = buildPreparedMenu(51L, 5.0);

        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-3")).thenReturn(List.of());
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-3")).thenReturn(List.of());
        when(preparedItemStockRepository.findPreparedStockDeductionsByOrderId("order-3"))
                .thenReturn(List.of(preparedDeduction(51L, 2.0)));
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-3")).thenReturn(List.of());
        when(saleItemRepository.findDistinctPreparedMenuIdsByOrderId("order-3")).thenReturn(List.of(51L));
        when(menuItemRepository.findAllByIdIn(anyCollection())).thenReturn(List.of(preparedMenu));
        when(preparedItemStockRepository.deductPreparedStockIfAvailable(org.mockito.ArgumentMatchers.eq(51L), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(1);

        inventoryService.deductStockForOrder(order);

        verify(preparedItemStockRepository).deductPreparedStockIfAvailable(org.mockito.ArgumentMatchers.eq(51L), anyDouble(), any(LocalDateTime.class));
        verify(menuItemRepository).saveAll(anyCollection());
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

    private com.kritik.POS.inventory.projection.PreparedStockDeductionProjection preparedDeduction(Long menuItemId, Double quantity) {
        return new com.kritik.POS.inventory.projection.PreparedStockDeductionProjection() {
            @Override
            public Long getMenuItemId() {
                return menuItemId;
            }

            @Override
            public Double getQuantity() {
                return quantity;
            }
        };
    }

    private MenuItem buildDirectMenu(Long id, String sku) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);
        menuItem.setHasRecipe(false);

        ItemStock itemStock = new ItemStock();
        itemStock.setSku(sku);
        itemStock.setIsActive(true);
        itemStock.setTotalStock(10);
        itemStock.setMenuItem(menuItem);
        menuItem.setItemStock(itemStock);
        return menuItem;
    }

    private MenuItem buildRecipeMenu(Long id) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);
        menuItem.setHasRecipe(true);
        menuItem.setIngredientUsages(List.of());
        return menuItem;
    }

    private MenuItem buildPreparedMenu(Long id, Double availableQty) {
        MenuItem menuItem = buildRecipeMenu(id);
        menuItem.setIsPrepared(true);

        PreparedItemStock preparedItemStock = new PreparedItemStock();
        preparedItemStock.setMenuItemId(id);
        preparedItemStock.setAvailableQty(availableQty);
        preparedItemStock.setReservedQty(0.0);
        preparedItemStock.setActive(true);
        menuItem.setPreparedItemStock(preparedItemStock);
        return menuItem;
    }
}
