package com.kritik.POS.order.service.Impl;

import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.OrderTax;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderService;
import com.kritik.POS.order.util.OrderUtil;
import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.entity.MenuItemIngredient;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.restaurant.util.InventoryAvailabilityUtil;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.service.TaxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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
    private final IngredientStockRepository ingredientStockRepository;
    private final InventoryService inventoryService;
    private final TaxService taxService;
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
        Order order = getAccessibleOrderWithItems(orderId);
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
    public PaymentProcessingResponse cancelTransaction(String orderId) {
        Order order = getAccessibleOrder(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cant cancel at this moment");
        }
        order.setPaymentStatus(PaymentStatus.PAYMENT_CANCELED);
        Order saveOrder = orderRepository.save(order);
        return new PaymentProcessingResponse(
                saveOrder,
                "Payment Canceled",
                "Your payment has been canceled. You can start adding items to your order again when you're ready."
        );
    }

    @Transactional
    @Override
    public PaymentProcessingResponse completePayment(String orderId, PaymentType paymentType) {
        Order order = getAccessibleOrder(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Payment cannot be completed in the current state");
        }
        order.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        order.setPaymentType(paymentType);
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
    public PaymentProcessingResponse refundPayment(String orderId) {
        Order order = getAccessibleOrder(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            if (Duration.between(order.getPaymentInitiatedTime(), LocalDateTime.now()).toHours() > 24) {
                throw new OrderException("Can't complete the refund");
            }
            order.setPaymentStatus(PaymentStatus.PAYMENT_REFUND);
            Order save = orderRepository.save(order);
            return new PaymentProcessingResponse(save, "Order Refunded", "Order refunded successfully please update the stock");
        }
        throw new OrderException("Payment has not been completed yet");
    }

    private void rebuildOrder(Order order, InitiateOrderRequest initiateOrderRequest) {
        List<SaleItem> saleItemList = new ArrayList<>();
        List<StockRequest> stockRequestList = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Long orderRestaurantId = null;

        for (InitiateOrderRequest.OrderItemRequest orderItemRequest : initiateOrderRequest.getOrderItems()) {
            MenuItem menuItem = inventoryService.getAccessibleMenuItem(orderItemRequest.menuItemId());
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, menuItem);

            SaleItem saleItem = new SaleItem();
            saleItem.setAmount(orderItemRequest.amount());
            saleItem.setSaleItemName(menuItem.getItemName());
            saleItem.setSaleItemPrice(RestaurantUtil.getMenuItemPrice(menuItem.getItemPrice()));
            saleItem.setMenuItem(menuItem);
            saleItem.setRestaurantId(menuItem.getRestaurantId());
            if (InventoryAvailabilityUtil.hasRecipe(menuItem)) {
                for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
                    ingredientRequirements.merge(
                            ingredientUsage.getIngredientStock().getSku(),
                            ingredientUsage.getQuantityRequired() * saleItem.getAmount(),
                            Double::sum
                    );
                }
            } else if (menuItem.getItemStock() != null) {
                stockRequestList.add(new StockRequest(menuItem.getItemStock().getSku(), saleItem.getAmount()));
            }
            saleItem.setOrder(order);
            saleItemList.add(saleItem);
        }

        inventoryService.checkStockAvailable(stockRequestList);
        checkIngredientStockAvailable(ingredientRequirements);
        order.setOrderItemList(saleItemList);
        order.setRestaurantId(orderRestaurantId);
        order.setOrderTaxes(buildOrderTaxes(order, orderRestaurantId));

        Double totalPrice = OrderUtil.getTotalPrice(order);
        if (totalPrice <= 0) {
            throw new AppException("Unable to calculate the price", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        order.setTotalPrice(totalPrice);
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

    private List<OrderTax> buildOrderTaxes(Order order, Long orderRestaurantId) {
        List<TaxRate> allActiveTaxs = taxService.getActiveTaxRates();
        if (allActiveTaxs == null || allActiveTaxs.isEmpty()) {
            return new ArrayList<>();
        }

        List<OrderTax> orderTaxes = new ArrayList<>();
        for (TaxRate activeTax : allActiveTaxs) {
            if (orderRestaurantId != null && !orderRestaurantId.equals(activeTax.getRestaurantId())) {
                continue;
            }
            OrderTax orderTax = new OrderTax();
            orderTax.setOrder(order);
            orderTax.setTaxAmount(activeTax.getTaxAmount());
            orderTax.setTaxName(activeTax.getTaxName());
            orderTaxes.add(orderTax);
        }
        return orderTaxes;
    }

    private PaymentType resolvePaymentType(PaymentType requestedPaymentType, PaymentType fallbackPaymentType) {
        return requestedPaymentType == null ? fallbackPaymentType : requestedPaymentType;
    }


    private Order getAccessibleOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId).orElseThrow(OrderException::new);
        validateAccessibleOrder(order);
        return order;
    }

    private Order getAccessibleOrderWithItems(String orderId) {
        Order order = orderRepository.findByOrderIdWithItems(orderId).orElseThrow(OrderException::new);
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

    private void checkIngredientStockAvailable(Map<String, Double> ingredientRequirements) {
        if (ingredientRequirements.isEmpty()) {
            return;
        }
        List<IngredientStock> ingredients = ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(ingredientRequirements.keySet());
        Map<String, IngredientStock> ingredientMap = new HashMap<>();
        for (IngredientStock ingredient : ingredients) {
            if (!tenantAccessService.isSuperAdmin()) {
                tenantAccessService.resolveAccessibleRestaurantId(ingredient.getRestaurantId());
            }
            ingredientMap.put(ingredient.getSku(), ingredient);
        }

        for (Map.Entry<String, Double> entry : ingredientRequirements.entrySet()) {
            IngredientStock ingredient = ingredientMap.get(entry.getKey());
            if (ingredient == null) {
                throw new StockException("Ingredient stock not found");
            }
            if (!Boolean.TRUE.equals(ingredient.getIsActive())) {
                throw new StockException(ingredient.getIngredientName() + " stock is inactive.");
            }
            if (ingredient.getTotalStock() == null || ingredient.getTotalStock() < entry.getValue()) {
                throw new StockException(ingredient.getIngredientName() + " is not available in stock only " + ingredient.getTotalStock() + " left.");
            }
        }
    }
}
