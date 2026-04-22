package com.kritik.POS.order.service.Impl;

import com.kritik.POS.common.util.MoneyUtils;
import com.kritik.POS.exception.errors.AppException;
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
import com.kritik.POS.order.service.OrderService;
import com.kritik.POS.order.service.internal.OrderMenuSupport;
import com.kritik.POS.order.service.internal.OrderStockDemand;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.api.TaxApi;
import com.kritik.POS.tax.api.TaxClassSnapshot;
import com.kritik.POS.tax.api.TaxableChargeComponent;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final MenuCatalogApi menuCatalogApi;
    private final InventoryApi inventoryApi;
    private final TaxApi taxApi;
    private final OrderPricingService orderPricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public PaymentProcessingResponse initiateOrder(InitiateOrderRequest initiateOrderRequest) {
        Order order = new Order();
        rebuildOrder(order, initiateOrderRequest);
        order.setPaymentType(resolvePaymentType(initiateOrderRequest.getPaymentType(), PaymentType.CASH));
        order.setPaymentInitiatedTime(LocalDateTime.now());
        order.setOrderId(UUID.randomUUID().toString());
        Order savedOrder = orderRepository.save(order);

        return new PaymentProcessingResponse(
                savedOrder,
                "Payment Initiation Complete",
                "Your order is ready for payment. Please select your preferred payment method to continue."
        );
    }

    @Override
    @Transactional
    public PaymentProcessingResponse updateOrder(String orderId, InitiateOrderRequest initiateOrderRequest) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cart can only be updated before payment confirmation", HttpStatus.BAD_REQUEST);
        }

        rebuildOrder(order, initiateOrderRequest);
        order.setPaymentType(resolvePaymentType(initiateOrderRequest.getPaymentType(), order.getPaymentType()));
        Order savedOrder = orderRepository.save(order);
        return new PaymentProcessingResponse(
                savedOrder,
                "Order Updated",
                "Your cart has been updated while payment is still pending confirmation."
        );
    }

    @Override
    @Transactional
    public PaymentProcessingResponse cancelTransaction(String orderId) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cant cancel at this moment");
        }
        order.setPaymentStatus(PaymentStatus.PAYMENT_CANCELED);
        order.setCancelledAt(LocalDateTime.now());
        Order saveOrder = orderRepository.save(order);
        return new PaymentProcessingResponse(
                saveOrder,
                "Payment Canceled",
                "Your payment has been canceled. You can start adding items to your order again when you're ready."
        );
    }

    @Transactional
    @Override
    public PaymentProcessingResponse completePayment(String orderId, CompletePaymentRequest request) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            return new PaymentProcessingResponse(
                    order,
                    "Payment Already Completed",
                    "This order was already marked as paid. Returning the existing payment result."
            );
        }
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Payment cannot be completed in the current state");
        }

        OrderStockDemand demand = buildPersistedStockDemand(order);
        inventoryApi.checkOrderStockAvailability(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements()
        );
        inventoryApi.deductStockForRequirements(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements(),
                demand.affectedMenuIds()
        );

        PaymentType paymentType = request == null ? null : request.getPaymentType();
        order.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        order.setPaymentType(resolvePaymentType(paymentType, order.getPaymentType()));
        order.setPaymentCompletedAt(LocalDateTime.now());
        applyPaymentAudit(order, request);
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getOrderId()));
        return new PaymentProcessingResponse(
                savedOrder,
                "Thank You!",
                "Your transaction is complete. Thank you for your purchase!"
        );
    }

    @Transactional
    @Override
    public PaymentProcessingResponse refundPayment(String orderId, RefundPaymentRequest request) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_REFUND)) {
            return new PaymentProcessingResponse(
                    order,
                    "Order Already Refunded",
                    "This order has already been refunded. Returning the current refund state."
            );
        }
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            throw new OrderException("Payment has not been completed yet");
        }

        LocalDateTime paymentTime = order.getPaymentCompletedAt() != null
                ? order.getPaymentCompletedAt()
                : order.getPaymentInitiatedTime();
        if (paymentTime != null && Duration.between(paymentTime, LocalDateTime.now()).toHours() > 24) {
            throw new OrderException("Can't complete the refund");
        }

        OrderStockDemand demand = buildPersistedStockDemand(order);
        inventoryApi.restoreStockForRequirements(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements(),
                demand.affectedMenuIds()
        );
        order.setPaymentStatus(PaymentStatus.PAYMENT_REFUND);
        order.setRefundedAt(LocalDateTime.now());
        applyRefundAudit(order, request);
        Order save = orderRepository.save(order);
        return new PaymentProcessingResponse(
                save,
                "Order Refunded",
                "Order refunded successfully and stock has been restored."
        );
    }

    private void rebuildOrder(Order order, InitiateOrderRequest initiateOrderRequest) {
        List<SaleItem> saleItemList = new ArrayList<>();
        OrderStockDemand demand = OrderStockDemand.empty();
        List<OrderPricingService.LinePricingPlan> linePlans = new ArrayList<>();
        Long orderRestaurantId = null;
        int lineIndex = 0;

        for (InitiateOrderRequest.OrderItemRequest orderItemRequest : initiateOrderRequest.getOrderItems()) {
            MenuItemSnapshot menuItem = menuCatalogApi.getAccessibleMenuItem(orderItemRequest.menuItemId());
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, menuItem.restaurantId());
            TaxClassSnapshot taxClass = taxApi.resolveTaxClass(menuItem.restaurantId(), menuItem.taxClassId());

            SaleItem saleItem = new SaleItem();
            saleItem.setAmount(orderItemRequest.amount());
            saleItem.setSaleItemName(menuItem.itemName());
            saleItem.setMenuItemId(menuItem.id());
            saleItem.setRestaurantId(menuItem.restaurantId());
            OrderMenuSupport.accumulateStockRequirements(menuItem, saleItem.getAmount(), demand);
            saleItem.setOrder(order);
            saleItemList.add(saleItem);

            BigDecimal quantity = BigDecimal.valueOf(orderItemRequest.amount());
            BigDecimal unitListAmount = menuItem.price().listPrice();
            BigDecimal unitDiscountedAmount = menuItem.price().discountedPrice();
            BigDecimal unitDiscountAmount = MoneyUtils.subtract(unitListAmount, unitDiscountedAmount);
            BigDecimal lineSubtotalAmount = MoneyUtils.multiply(unitListAmount, quantity);
            BigDecimal lineDiscountAmount = MoneyUtils.multiply(unitDiscountAmount, quantity);
            String referenceKey = "sale-" + lineIndex++;
            linePlans.add(OrderPricingService.LinePricingPlan.forSaleItem(
                    referenceKey,
                    taxClass.code(),
                    menuItem.price().priceIncludesTax(),
                    unitListAmount,
                    unitDiscountAmount,
                    lineSubtotalAmount,
                    lineDiscountAmount,
                    List.of(new TaxableChargeComponent(
                            referenceKey,
                            taxClass.code(),
                            taxClass.id(),
                            MoneyUtils.multiply(unitDiscountedAmount, quantity),
                            menuItem.price().priceIncludesTax()
                    )),
                    saleItem
            ));
        }

        inventoryApi.checkOrderStockAvailability(demand.stockRequests(), demand.ingredientRequirements(), demand.preparedRequirements());
        replaceOrderItems(order, saleItemList);
        order.setRestaurantId(orderRestaurantId);
        orderPricingService.applyPricing(order, orderRestaurantId, linePlans, initiateOrderRequest.getTaxContext());
        if (order.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Unable to calculate the price", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long validateRestaurant(Order order, Long orderRestaurantId, Long menuRestaurantId) {
        if (orderRestaurantId == null) {
            orderRestaurantId = menuRestaurantId;
        } else if (!orderRestaurantId.equals(menuRestaurantId)) {
            throw new AppException("All order items must belong to the same restaurant", HttpStatus.BAD_REQUEST);
        }

        if (order.getRestaurantId() != null && !order.getRestaurantId().equals(orderRestaurantId)) {
            throw new AppException("Cart items must stay in the same restaurant as the original order", HttpStatus.BAD_REQUEST);
        }
        return orderRestaurantId;
    }

    private PaymentType resolvePaymentType(PaymentType requestedPaymentType, PaymentType fallbackPaymentType) {
        return requestedPaymentType == null ? fallbackPaymentType : requestedPaymentType;
    }

    private void replaceOrderItems(Order order, List<SaleItem> saleItemList) {
        List<SaleItem> managedItems = order.getOrderItemList();
        if (managedItems == null) {
            managedItems = new ArrayList<>();
            order.setOrderItemList(managedItems);
        }
        managedItems.clear();
        managedItems.addAll(saleItemList);
    }

    private OrderStockDemand buildPersistedStockDemand(Order order) {
        OrderStockDemand demand = OrderStockDemand.empty();
        for (SaleItem saleItem : order.getOrderItemList()) {
            MenuItemSnapshot menuItem = menuCatalogApi.getAccessibleMenuItem(saleItem.getMenuItemId());
            OrderMenuSupport.accumulateStockRequirements(menuItem, saleItem.getAmount(), demand);
        }
        return demand;
    }

    private void applyPaymentAudit(Order order, CompletePaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setPaymentReference(OrderMenuSupport.trimToNull(request == null ? null : request.getPaymentReference()));
        order.setPaymentCollectedBy(OrderMenuSupport.trimToNull(
                request != null && request.getPaymentCollectedBy() != null
                        ? request.getPaymentCollectedBy()
                        : currentUser != null ? currentUser.getEmail() : null
        ));
        order.setPaymentNotes(OrderMenuSupport.trimToNull(request == null ? null : request.getPaymentNotes()));
        order.setExternalTxnId(OrderMenuSupport.trimToNull(request == null ? null : request.getExternalTxnId()));
    }

    private void applyRefundAudit(Order order, RefundPaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setRefundOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setRefundReason(OrderMenuSupport.trimToNull(request == null ? null : request.getRefundReason()));
        order.setRefundNotes(OrderMenuSupport.trimToNull(request == null ? null : request.getRefundNotes()));
    }

    private SecurityUser resolveCurrentUser() {
        try {
            return tenantAccessService.currentUser();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Order getAccessibleOrderWithItemsForUpdate(String orderId) {
        Order order = orderRepository.findByOrderIdWithItemsForUpdate(orderId).orElseThrow(OrderException::new);
        validateAccessibleOrder(order);
        return order;
    }

    private void validateAccessibleOrder(Order order) {
        if (order.isDeleted()) {
            throw new OrderException("Order not found");
        }
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(order.getRestaurantId());
        }
    }
}
