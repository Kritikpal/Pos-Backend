package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.service.TaxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
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
    private OrderRepository orderRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private InventoryUtil inventoryUtil;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void applyStockChangesForCompletedOrderUsesBulkQueriesAndRefreshesAffectedMenus() {
        Order order = new Order();
        order.setOrderId("order-1");
        order.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);

        when(orderRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));
        when(saleItemRepository.findDirectStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(directDeduction("SKU-1", 3L)));
        when(saleItemRepository.findIngredientStockDeductionsByOrderId("order-1"))
                .thenReturn(List.of(ingredientDeduction("ING-1", 2.5)));
        when(saleItemRepository.findDistinctDirectMenuIdsByOrderId("order-1")).thenReturn(List.of(11L, 12L));
        when(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(anyCollection())).thenReturn(List.of(21L, 22L));
        when(stockRepository.deductStockQuantity(anyString(), anyInt(), any(LocalDateTime.class))).thenReturn(1);
        when(ingredientStockRepository.deductStockQuantity(anyString(), any(Double.class), any(LocalDateTime.class))).thenReturn(1);

        inventoryService.applyStockChangesForCompletedOrder("order-1");

        verify(stockRepository).deductStockQuantity(anyString(), anyInt(), any(LocalDateTime.class));
        verify(ingredientStockRepository).deductStockQuantity(anyString(), any(Double.class), any(LocalDateTime.class));
        verify(menuItemRepository).markUnavailableByIds(List.of(11L, 12L));
        verify(menuItemRepository).markDirectMenusAvailableByIds(List.of(11L, 12L));
        verify(menuItemRepository).refreshRecipeAvailabilityByIds(List.of(21L, 22L));
    }

    @Test
    void applyStockChangesForCompletedOrderSkipsPendingOrders() {
        Order order = new Order();
        order.setOrderId("order-2");
        order.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);

        when(orderRepository.findByOrderId("order-2")).thenReturn(Optional.of(order));

        inventoryService.applyStockChangesForCompletedOrder("order-2");

        verify(saleItemRepository, never()).findDirectStockDeductionsByOrderId(anyString());
        verify(stockRepository, never()).deductStockQuantity(anyString(), anyInt(), any(LocalDateTime.class));
        verify(menuItemRepository, never()).markUnavailableByIds(anyCollection());
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
