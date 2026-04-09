package com.kritik.POS.order.service.Impl;

import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.service.TaxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TaxService taxService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void updateOrderAllowsRemovingItemsWithoutReplacingManagedCollection() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-1");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentType(PaymentType.UPI);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);
        List<SaleItem> managedItems = new ArrayList<>();
        managedItems.add(buildSaleItem(existingOrder, buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100"), 1));
        managedItems.add(buildSaleItem(existingOrder, buildMenuItem(101L, 10L, "Fries", 30.0, "SKU-101"), 2));
        existingOrder.setOrderItemList(managedItems);

        MenuItem menuItem = buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100");
        InitiateOrderRequest request = buildRequest(100L, 3, PaymentType.CARD);

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-1")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.getAccessibleMenuItem(100L)).thenReturn(menuItem);
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        PaymentProcessingResponse response = orderService.updateOrder("order-1", request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(savedOrder.getOrderItemList()).isSameAs(managedItems);
        assertThat(savedOrder.getOrderItemList()).hasSize(1);
        SaleItem savedItem = savedOrder.getOrderItemList().get(0);
        assertThat(savedItem.getAmount()).isEqualTo(3);
        assertThat(savedItem.getSaleItemName()).isEqualTo("Burger");
        assertThat(response.getOrderId()).isEqualTo("order-1");
        assertThat(response.getPaymentType()).isEqualTo(PaymentType.CARD);
        assertThat(response.getTotalPrice()).isEqualTo(150.0);
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
        existingOrder.setOrderItemList(List.of(
                buildSaleItem(existingOrder, buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100"), 2)
        ));
        existingOrder.setTotalPrice(100.0);

        CompletePaymentRequest request = new CompletePaymentRequest(
                PaymentType.UPI,
                "POS-REF-001",
                null,
                "Paid at front desk",
                "UPI-123"
        );

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-3")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.getAccessibleMenuItem(100L)).thenReturn(buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100"));
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

        PaymentProcessingResponse response = orderService.completePayment("order-3", request);

        verify(inventoryService).deductStockForOrder(existingOrder);
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
    void completePaymentReturnsExistingStateWhenOrderAlreadyPaid() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-4");
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        existingOrder.setPaymentType(PaymentType.CARD);
        existingOrder.setPaymentCompletedAt(LocalDateTime.now().minusMinutes(5));

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-4")).thenReturn(Optional.of(existingOrder));
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        PaymentProcessingResponse response = orderService.completePayment(
                "order-4",
                new CompletePaymentRequest(PaymentType.CARD, null, null, null, null)
        );

        verify(inventoryService, never()).deductStockForOrder(any(Order.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishEvent(any(OrderCompletedEvent.class));
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_SUCCESSFUL);
    }

    @Test
    void refundPaymentRestoresStockAndCapturesAuditMetadata() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-5");
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        existingOrder.setPaymentCompletedAt(LocalDateTime.now().minusHours(1));
        existingOrder.setOrderItemList(List.of(
                buildSaleItem(existingOrder, buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100"), 1)
        ));

        when(orderRepository.findByOrderIdWithItemsForUpdate("order-5")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
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

        verify(inventoryService).restoreStockForRefund(existingOrder);
        verify(orderRepository).save(existingOrder);
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_REFUND);
        assertThat(response.getRefundReason()).isEqualTo("Customer changed mind");
        assertThat(response.getRefundNotes()).isEqualTo("Refunded at counter");
        assertThat(response.getRefundOperatorUserId()).isEqualTo(8L);
        assertThat(response.getRefundedAt()).isNotNull();
    }

    @Test
    void initiateOrderUsesRecipeBatchSizeForIngredientValidation() {
        MenuItem recipeMenu = buildRecipeMenuItem(200L, 10L, "Biryani", 180.0, "ING-1", 20.0, 10);

        when(inventoryService.getAccessibleMenuItem(200L)).thenReturn(recipeMenu);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taxService.getActiveTaxRates()).thenReturn(List.of());

        PaymentProcessingResponse response = orderService.initiateOrder(buildRequest(200L, 5, PaymentType.CARD));

        assertThat(response.getTotalPrice()).isEqualTo(900.0);
        verify(orderRepository).save(any(Order.class));
        verify(inventoryService).checkOrderStockAvailability(any(List.class), argThat(requirements ->
                requirements.containsKey("ING-1") && Math.abs(requirements.get("ING-1") - 10.0) < 0.0001
        ), eq(Map.<Long, Double>of()));
    }

    @Test
    void initiateOrderUsesPreparedStockInsteadOfIngredientStockForPreparedItems() {
        MenuItem preparedMenu = buildPreparedMenuItem(300L, 10L, "Paneer Roll", 120.0, "ING-2", 5.0, 5, 8.0);

        when(inventoryService.getAccessibleMenuItem(300L)).thenReturn(preparedMenu);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taxService.getActiveTaxRates()).thenReturn(List.of());

        PaymentProcessingResponse response = orderService.initiateOrder(buildRequest(300L, 3, PaymentType.CASH));

        assertThat(response.getTotalPrice()).isEqualTo(360.0);
        verify(inventoryService).checkOrderStockAvailability(eq(List.of()), eq(Map.<String, Double>of()), argThat(requirements ->
                requirements.containsKey(300L) && Math.abs(requirements.get(300L) - 3.0) < 0.0001
        ));
    }

    private InitiateOrderRequest buildRequest(Long menuItemId, Integer amount, PaymentType paymentType) {
        InitiateOrderRequest request = new InitiateOrderRequest();
        request.setOrderItems(List.of(new InitiateOrderRequest.OrderItemRequest(menuItemId, amount)));
        request.setPaymentType(paymentType);
        return request;
    }

    private SaleItem buildSaleItem(Order order, MenuItem menuItem, Integer amount) {
        SaleItem saleItem = new SaleItem();
        saleItem.setOrder(order);
        saleItem.setMenuItem(menuItem);
        saleItem.setAmount(amount);
        saleItem.setRestaurantId(menuItem.getRestaurantId());
        saleItem.setSaleItemName(menuItem.getItemName());
        saleItem.setSaleItemPrice(menuItem.getItemPrice().getPrice());
        return saleItem;
    }

    private MenuItem buildMenuItem(Long id, Long restaurantId, String itemName, Double price, String sku) {
        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(price);
        itemPrice.setDisCount(0.0);

        ItemStock itemStock = new ItemStock();
        itemStock.setSku(sku);
        itemStock.setTotalStock(20);
        itemStock.setIsActive(true);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setItemName(itemName);
        menuItem.setItemPrice(itemPrice);
        menuItem.setItemStock(itemStock);
        menuItem.setHasRecipe(false);
        return menuItem;
    }

    private MenuItem buildRecipeMenuItem(Long id,
                                         Long restaurantId,
                                         String itemName,
                                         Double price,
                                         String ingredientSku,
                                         Double quantityRequired,
                                         Integer batchSize) {
        MenuItem menuItem = buildMenuItem(id, restaurantId, itemName, price, null);
        menuItem.setItemStock(null);
        menuItem.setHasRecipe(true);

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku(ingredientSku);
        ingredientStock.setIngredientName("Rice");
        ingredientStock.setRestaurantId(restaurantId);
        ingredientStock.setIsActive(true);
        ingredientStock.setIsDeleted(false);
        ingredientStock.setTotalStock(50.0);
        ingredientStock.setReorderLevel(5.0);

        MenuRecipe recipe = new MenuRecipe();
        recipe.setMenuItem(menuItem);
        recipe.setBatchSize(batchSize);

        MenuItemIngredient ingredientUsage = new MenuItemIngredient();
        ingredientUsage.setMenuItem(menuItem);
        ingredientUsage.setRecipe(recipe);
        ingredientUsage.setIngredientStock(ingredientStock);
        ingredientUsage.setQuantityRequired(quantityRequired);

        recipe.setIngredientUsages(List.of(ingredientUsage));
        menuItem.setRecipe(recipe);
        menuItem.setIngredientUsages(List.of(ingredientUsage));
        return menuItem;
    }

    private MenuItem buildPreparedMenuItem(Long id,
                                           Long restaurantId,
                                           String itemName,
                                           Double price,
                                           String ingredientSku,
                                           Double quantityRequired,
                                           Integer batchSize,
                                           Double preparedQty) {
        MenuItem menuItem = buildRecipeMenuItem(id, restaurantId, itemName, price, ingredientSku, quantityRequired, batchSize);
        menuItem.setIsPrepared(true);

        PreparedItemStock preparedItemStock = new PreparedItemStock();
        preparedItemStock.setMenuItemId(id);
        preparedItemStock.setRestaurantId(restaurantId);
        preparedItemStock.setAvailableQty(preparedQty);
        preparedItemStock.setReservedQty(0.0);
        preparedItemStock.setUnitCode("PCS");
        preparedItemStock.setActive(true);
        menuItem.setPreparedItemStock(preparedItemStock);
        return menuItem;
    }
}
