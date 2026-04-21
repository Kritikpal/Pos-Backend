package com.kritik.POS.order.service.Impl;

import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderService;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import com.kritik.POS.tax.service.TaxService;
import com.kritik.POS.tax.util.MoneyUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final TaxService taxService;
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
        validateCurrentOrderStock(order);
        inventoryService.deductStockForOrder(order);

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

        inventoryService.restoreStockForRefund(order);
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
        List<StockRequest> stockRequestList = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        List<OrderPricingService.LinePricingPlan> linePlans = new ArrayList<>();
        Long orderRestaurantId = null;
        int lineIndex = 0;

        for (InitiateOrderRequest.OrderItemRequest orderItemRequest : initiateOrderRequest.getOrderItems()) {
            MenuItem menuItem = inventoryService.getAccessibleMenuItem(orderItemRequest.menuItemId());
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, menuItem);
            TaxClass taxClass = taxService.resolveTaxClass(menuItem.getRestaurantId(), menuItem.getTaxClassId());

            SaleItem saleItem = new SaleItem();
            saleItem.setAmount(orderItemRequest.amount());
            saleItem.setSaleItemName(menuItem.getItemName());
            saleItem.setMenuItem(menuItem);
            saleItem.setRestaurantId(menuItem.getRestaurantId());
            InventoryAvailabilityUtil.accumulateStockRequirements(
                    menuItem,
                    saleItem.getAmount(),
                    stockRequestList,
                    ingredientRequirements,
                    preparedRequirements
            );
            saleItem.setOrder(order);
            saleItemList.add(saleItem);

            BigDecimal quantity = BigDecimal.valueOf(orderItemRequest.amount());
            BigDecimal unitListAmount = MoneyUtils.money(menuItem.getItemPrice().getPrice());
            BigDecimal unitDiscountedAmount = RestaurantUtil.getMenuItemPrice(menuItem.getItemPrice());
            BigDecimal unitDiscountAmount = MoneyUtils.subtract(unitListAmount, unitDiscountedAmount);
            BigDecimal lineSubtotalAmount = MoneyUtils.multiply(unitListAmount, quantity);
            BigDecimal lineDiscountAmount = MoneyUtils.multiply(unitDiscountAmount, quantity);
            String referenceKey = "sale-" + lineIndex++;
            linePlans.add(OrderPricingService.LinePricingPlan.forSaleItem(
                    referenceKey,
                    taxClass.getCode(),
                    Boolean.TRUE.equals(menuItem.getItemPrice().getPriceIncludesTax()),
                    unitListAmount,
                    unitDiscountAmount,
                    lineSubtotalAmount,
                    lineDiscountAmount,
                    List.of(new TaxableChargeComponent(
                            referenceKey,
                            taxClass.getCode(),
                            taxClass.getId(),
                            MoneyUtils.multiply(unitDiscountedAmount, quantity),
                            Boolean.TRUE.equals(menuItem.getItemPrice().getPriceIncludesTax())
                    )),
                    saleItem
            ));
        }

        inventoryService.checkOrderStockAvailability(stockRequestList, ingredientRequirements, preparedRequirements);
        replaceOrderItems(order, saleItemList);
        order.setRestaurantId(orderRestaurantId);
        orderPricingService.applyPricing(order, orderRestaurantId, linePlans, initiateOrderRequest.getTaxContext());
        if (order.getGrandTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Unable to calculate the price", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Long validateRestaurant(Order order, Long orderRestaurantId, MenuItem menuItem) {
        if (orderRestaurantId == null) {
            orderRestaurantId = menuItem.getRestaurantId();
        } else if (!orderRestaurantId.equals(menuItem.getRestaurantId())) {
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

    private void validateCurrentOrderStock(Order order) {
        List<StockRequest> stockRequestList = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();

        for (SaleItem saleItem : order.getOrderItemList()) {
            MenuItem menuItem = inventoryService.getAccessibleMenuItem(saleItem.getMenuItem().getId());
            InventoryAvailabilityUtil.accumulateStockRequirements(
                    menuItem,
                    saleItem.getAmount(),
                    stockRequestList,
                    ingredientRequirements,
                    preparedRequirements
            );
        }

        inventoryService.checkOrderStockAvailability(stockRequestList, ingredientRequirements, preparedRequirements);
    }

    private void applyPaymentAudit(Order order, CompletePaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setPaymentReference(trimToNull(request == null ? null : request.getPaymentReference()));
        order.setPaymentCollectedBy(trimToNull(
                request != null && request.getPaymentCollectedBy() != null
                        ? request.getPaymentCollectedBy()
                        : currentUser != null ? currentUser.getEmail() : null
        ));
        order.setPaymentNotes(trimToNull(request == null ? null : request.getPaymentNotes()));
        order.setExternalTxnId(trimToNull(request == null ? null : request.getExternalTxnId()));
    }

    private void applyRefundAudit(Order order, RefundPaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setRefundOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setRefundReason(trimToNull(request == null ? null : request.getRefundReason()));
        order.setRefundNotes(trimToNull(request == null ? null : request.getRefundNotes()));
    }

    private SecurityUser resolveCurrentUser() {
        try {
            return tenantAccessService.currentUser();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return InventoryUtil.trimToNull(value);
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
