package com.kritik.POS.order.service.Impl;

import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.service.TaxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TaxService taxService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void updateOrderAllowsCartChangesBeforePaymentConfirmation() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-1");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentType(PaymentType.UPI);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_INITIATED);

        MenuItem menuItem = buildMenuItem(100L, 10L, "Burger", 50.0, "SKU-100");
        InitiateOrderRequest request = buildRequest(100L, 3, PaymentType.CARD);

        when(orderRepository.findByOrderIdWithItems("order-1")).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryService.getAccessibleMenuItem(100L)).thenReturn(menuItem);
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        PaymentProcessingResponse response = orderService.updateOrder("order-1", request);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertThat(response.getOrderId()).isEqualTo("order-1");
        assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.PAYMENT_INITIATED);
        assertThat(response.getPaymentType()).isEqualTo(PaymentType.CARD);
        assertThat(response.getTotalPrice()).isEqualTo(150.0);
        assertThat(savedOrder.getOrderItemList()).hasSize(1);
        SaleItem savedItem = savedOrder.getOrderItemList().get(0);
        assertThat(savedItem.getAmount()).isEqualTo(3);
        assertThat(savedItem.getSaleItemName()).isEqualTo("Burger");
        assertThat(savedItem.getSaleItemPrice()).isEqualTo(50.0);
    }

    @Test
    void updateOrderRejectsChangesAfterPaymentCompletion() {
        Order existingOrder = new Order();
        existingOrder.setOrderId("order-2");
        existingOrder.setRestaurantId(10L);
        existingOrder.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);

        when(orderRepository.findByOrderIdWithItems("order-2")).thenReturn(Optional.of(existingOrder));
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

    private InitiateOrderRequest buildRequest(Long menuItemId, Integer amount, PaymentType paymentType) {
        InitiateOrderRequest request = new InitiateOrderRequest();
        request.setOrderItems(List.of(new InitiateOrderRequest.OrderItemRequest(menuItemId, amount)));
        request.setPaymentType(paymentType);
        return request;
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
}
