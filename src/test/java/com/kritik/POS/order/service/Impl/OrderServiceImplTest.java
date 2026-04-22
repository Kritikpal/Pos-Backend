package com.kritik.POS.order.service.Impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.inventory.api.InventoryApi;
import com.kritik.POS.order.api.OrderCompletedEvent;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.restaurant.api.IngredientUsageSnapshot;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.restaurant.api.MenuItemType;
import com.kritik.POS.restaurant.api.MenuPriceSnapshot;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.api.TaxApi;
import com.kritik.POS.tax.api.TaxClassSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuCatalogApi menuCatalogApi;

    @Mock
    private InventoryApi inventoryApi;

    @Mock
    private TaxApi taxApi;

    @Mock
    private OrderPricingService orderPricingService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        when(taxApi.resolveTaxClass(any(), any())).thenReturn(new TaxClassSnapshot(1L, 10L, "GST", false));
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<OrderPricingService.LinePricingPlan> plans = invocation.getArgument(2);

            BigDecimal subtotal = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal grandTotal = BigDecimal.ZERO;

            for (OrderPricingService.LinePricingPlan plan : plans) {
                BigDecimal lineTotal = plan.lineSubtotalAmount().subtract(plan.lineDiscountAmount());
                subtotal = subtotal.add(plan.lineSubtotalAmount());
                discount = discount.add(plan.lineDiscountAmount());
                grandTotal = grandTotal.add(lineTotal);

                if (plan.saleItem() != null) {
                    SaleItem saleItem = plan.saleItem();
                    saleItem.setTaxClassCodeSnapshot(plan.taxClassCodeSnapshot());
                    saleItem.setPriceIncludesTax(plan.priceIncludesTax());
                    saleItem.setUnitListAmount(plan.unitListAmount());
                    saleItem.setUnitDiscountAmount(plan.unitDiscountAmount());
                    saleItem.setUnitTaxableAmount(lineTotal.divide(BigDecimal.valueOf(saleItem.getAmount())));
                    saleItem.setUnitTaxAmount(BigDecimal.ZERO);
                    saleItem.setUnitTotalAmount(lineTotal.divide(BigDecimal.valueOf(saleItem.getAmount())));
                    saleItem.setLineSubtotalAmount(plan.lineSubtotalAmount());
                    saleItem.setLineDiscountAmount(plan.lineDiscountAmount());
                    saleItem.setLineTaxableAmount(lineTotal);
                    saleItem.setLineTaxAmount(BigDecimal.ZERO);
                    saleItem.setLineTotalAmount(lineTotal);
                    saleItem.setSaleItemPrice(saleItem.getUnitTotalAmount());
                }
            }

            order.setSubtotalAmount(subtotal);
            order.setDiscountAmount(discount);
            order.setTaxableAmount(grandTotal);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setFeeAmount(BigDecimal.ZERO);
            order.setRoundingAmount(BigDecimal.ZERO);
            order.setGrandTotal(grandTotal);
            order.setTotalPrice(grandTotal);
            return null;
        }).when(orderPricingService).applyPricing(any(Order.class), any(), anyList(), any());
    }

    @Test
    void updateOrderAllowsRemovingItemsWithoutReplacingManagedCollection() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-1");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentType(PaymentType.UPI);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
        List<SaleItem> managedItems = new ArrayList<>();
        managedItems.add(buildSaleItem(existingOrder, 100L, 10L, "Burger", 50.00, 1));
        managedItems.add(buildSaleItem(existingOrder, 101L, 10L, "Fries", 30.00, 2));
        existingOrder.setOrderItemList(managedItems);

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-1")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuCatalogApi.getAccessibleMenuItem(100L)).thenReturn(directMenu(100L, 10L, "Burger", 50.00, "SKU-100"));
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        PaymentProcessingResponse response = orderService.updateOrder("order-1", buildRequest(100L, 3, PaymentType.CARD));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getOrderItemList()).isSameAs(managedItems);
        assertThat(savedOrder.getOrderItemList()).hasSize(1);
        assertThat(savedOrder.getOrderItemList().get(0).getAmount()).isEqualTo(3);
        assertThat(savedOrder.getOrderItemList().get(0).getSaleItemName()).isEqualTo("Burger");
        assertThat(response.getOrderId()).isEqualTo("order-1");
        assertThat(response.getPaymentType()).isEqualTo(PaymentType.CARD);
        assertThat(response.getTotalPrice()).isEqualByComparingTo("150.00");
    }

    @Test
    void updateOrderRejectsChangesAfterPaymentCompletion() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-2");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-2")).thenReturn(Optional.of(existingOrder));
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        assertThatThrownBy(() -> orderService.updateOrder("order-2", buildRequest(100L, 1, PaymentType.CASH)))
                .isInstanceOf(OrderException.class)
                .satisfies(error -> {
                    OrderException orderException = (OrderException) error;
                    assertThat(orderException.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(orderException.getMessage()).isEqualTo("Cart can only be updated before payment confirmation");
                });

        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void completePaymentDeductsStockOnceAndPublishesInvoiceEvent() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-3");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
        existingOrder.setPaymentType(PaymentType.CASH);
        existingOrder.setOrderItemList(List.of(buildSaleItem(existingOrder, 100L, 10L, "Burger", 50.00, 2)));
        existingOrder.setTotalPrice(new BigDecimal("100.00"));

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-3")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuCatalogApi.getAccessibleMenuItem(100L)).thenReturn(directMenu(100L, 10L, "Burger", 50.00, "SKU-100"));
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);
        when(tenantAccessService.currentUser()).thenReturn(new SecurityUser(
                7L,
                "cashier@example.com",
                "token",
                "token-id",
                10L,
                1L,
                Set.of("RESTAURANT_ADMIN")
        ));

        PaymentProcessingResponse response = orderService.completePayment(
                "order-3",
                new CompletePaymentRequest(PaymentType.UPI, "POS-REF-001", null, "Paid at front desk", "UPI-123")
        );

        verify(inventoryApi).checkOrderStockAvailability(anyList(), any(), any());
        verify(inventoryApi).deductStockForRequirements(anyList(), any(), any(), any());
        verify(orderRepository).save(existingOrder);
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESSFUL);
        assertThat(response.getPaymentType()).isEqualTo(PaymentType.UPI);
        assertThat(response.getPaymentReference()).isEqualTo("POS-REF-001");
        assertThat(response.getPaymentCollectedBy()).isEqualTo("cashier@example.com");
        assertThat(response.getExternalTxnId()).isEqualTo("UPI-123");
        assertThat(response.getOperatorUserId()).isEqualTo(7L);
        assertThat(response.getPaymentCompletedAt()).isNotNull();
    }

    @Test
    void refundPaymentRestoresStockAndCapturesAuditMetadata() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-5");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        existingOrder.setPaymentCompletedAt(LocalDateTime.now().minusHours(1));
        existingOrder.setOrderItemList(List.of(buildSaleItem(existingOrder, 100L, 10L, "Burger", 50.00, 1)));

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-5")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuCatalogApi.getAccessibleMenuItem(100L)).thenReturn(directMenu(100L, 10L, "Burger", 50.00, "SKU-100"));
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);
        when(tenantAccessService.currentUser()).thenReturn(new SecurityUser(
                8L,
                "manager@example.com",
                "token",
                "token-id",
                10L,
                1L,
                Set.of("RESTAURANT_ADMIN")
        ));

        PaymentProcessingResponse response = orderService.refundPayment(
                "order-5",
                new RefundPaymentRequest("Customer changed mind", "Refunded at counter")
        );

        verify(inventoryApi).restoreStockForRequirements(anyList(), any(), any(), any());
        verify(orderRepository).save(existingOrder);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_REFUND);
        assertThat(response.getRefundReason()).isEqualTo("Customer changed mind");
        assertThat(response.getRefundNotes()).isEqualTo("Refunded at counter");
        assertThat(response.getRefundOperatorUserId()).isEqualTo(8L);
        assertThat(response.getRefundedAt()).isNotNull();
    }

    @Test
    void initiateOrderUsesRecipeBatchSizeForIngredientValidation() {
        when(menuCatalogApi.getAccessibleMenuItem(200L)).thenReturn(recipeMenu(200L, 10L, "Biryani", 180.00, "ING-1", 20.0, 10));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentProcessingResponse response = orderService.initiateOrder(buildRequest(200L, 5, PaymentType.CARD));

        assertThat(response.getTotalPrice()).isEqualByComparingTo("900.00");
        verify(orderRepository).save(any(Order.class));
        verify(inventoryApi).checkOrderStockAvailability(eq(List.of()), any(), eq(java.util.Map.<Long, Double>of()));
        ArgumentCaptor<java.util.Map<String, Double>> ingredientCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(inventoryApi).checkOrderStockAvailability(eq(List.of()), ingredientCaptor.capture(), eq(java.util.Map.<Long, Double>of()));
        assertThat(ingredientCaptor.getValue()).containsEntry("ING-1", 10.0);
    }

    @Test
    void initiateOrderUsesPreparedStockInsteadOfIngredientStockForPreparedItems() {
        when(menuCatalogApi.getAccessibleMenuItem(300L)).thenReturn(preparedMenu(300L, 10L, "Paneer Roll", 120.00));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentProcessingResponse response = orderService.initiateOrder(buildRequest(300L, 3, PaymentType.CASH));

        assertThat(response.getTotalPrice()).isEqualByComparingTo("360.00");
        ArgumentCaptor<java.util.Map<Long, Double>> preparedCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(inventoryApi).checkOrderStockAvailability(eq(List.of()), eq(java.util.Map.<String, Double>of()), preparedCaptor.capture());
        assertThat(preparedCaptor.getValue()).containsEntry(300L, 3.0);
    }

    private InitiateOrderRequest buildRequest(Long menuItemId, Integer amount, PaymentType paymentType) {
        InitiateOrderRequest request = new InitiateOrderRequest();
        request.setOrderItems(List.of(new InitiateOrderRequest.OrderItemRequest(menuItemId, amount)));
        request.setPaymentType(paymentType);
        return request;
    }

    private SaleItem buildSaleItem(Order order, Long menuItemId, Long restaurantId, String itemName, double unitPrice, Integer amount) {
        SaleItem saleItem = new SaleItem();
        saleItem.setOrder(order);
        saleItem.setMenuItemId(menuItemId);
        saleItem.setAmount(amount);
        saleItem.setRestaurantId(restaurantId);
        saleItem.setSaleItemName(itemName);
        saleItem.setSaleItemPrice(BigDecimal.valueOf(unitPrice));
        saleItem.setTaxClassCodeSnapshot("GST");
        saleItem.setUnitListAmount(BigDecimal.valueOf(unitPrice));
        saleItem.setUnitDiscountAmount(BigDecimal.ZERO);
        saleItem.setUnitTaxableAmount(BigDecimal.valueOf(unitPrice));
        saleItem.setUnitTaxAmount(BigDecimal.ZERO);
        saleItem.setUnitTotalAmount(BigDecimal.valueOf(unitPrice));
        saleItem.setLineSubtotalAmount(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(amount)));
        saleItem.setLineDiscountAmount(BigDecimal.ZERO);
        saleItem.setLineTaxableAmount(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(amount)));
        saleItem.setLineTaxAmount(BigDecimal.ZERO);
        saleItem.setLineTotalAmount(BigDecimal.valueOf(unitPrice).multiply(BigDecimal.valueOf(amount)));
        return saleItem;
    }

    private MenuItemSnapshot directMenu(Long id, Long restaurantId, String itemName, double price, String sku) {
        return new MenuItemSnapshot(
                id,
                restaurantId,
                1L,
                itemName,
                itemName,
                true,
                false,
                true,
                MenuItemType.DIRECT,
                new MenuPriceSnapshot(BigDecimal.valueOf(price), BigDecimal.valueOf(price), BigDecimal.ZERO, false),
                sku,
                List.of()
        );
    }

    private MenuItemSnapshot recipeMenu(Long id, Long restaurantId, String itemName, double price, String ingredientSku, double quantityRequired, int batchSize) {
        return new MenuItemSnapshot(
                id,
                restaurantId,
                1L,
                itemName,
                itemName,
                true,
                false,
                true,
                MenuItemType.RECIPE,
                new MenuPriceSnapshot(BigDecimal.valueOf(price), BigDecimal.valueOf(price), BigDecimal.ZERO, false),
                null,
                List.of(new IngredientUsageSnapshot(ingredientSku, quantityRequired, batchSize))
        );
    }

    private MenuItemSnapshot preparedMenu(Long id, Long restaurantId, String itemName, double price) {
        return new MenuItemSnapshot(
                id,
                restaurantId,
                1L,
                itemName,
                itemName,
                true,
                false,
                true,
                MenuItemType.PREPARED,
                new MenuPriceSnapshot(BigDecimal.valueOf(price), BigDecimal.valueOf(price), BigDecimal.ZERO, false),
                null,
                List.of()
        );
    }
}
