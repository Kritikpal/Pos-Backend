package com.kritik.POS.order.service.Impl;

import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.OrderException;
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
import com.kritik.POS.restaurant.entity.ItemStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.StockRepository;
import com.kritik.POS.restaurant.service.StockService;
import com.kritik.POS.restaurant.specification.MenuItemSpecification;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.service.TaxService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final StockRepository stockRepository;
    private final StockService stockService;
    private final TaxService taxService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public PaymentProcessingResponse initiateOrder(InitiateOrderRequest initiateOrderRequest) {
        Order order = new Order();
        List<SaleItem> saleItemList = new ArrayList<>();
        List<StockRequest> stockRequestList = new ArrayList<>();
        Long orderRestaurantId = null;

        for (InitiateOrderRequest.OrderItemRequest orderItemRequest : initiateOrderRequest.getOrderItems()) {
            MenuItem menuItem = getAccessibleMenuItem(orderItemRequest.menuItemId());
            if (orderRestaurantId == null) {
                orderRestaurantId = menuItem.getRestaurantId();
            } else if (!orderRestaurantId.equals(menuItem.getRestaurantId())) {
                throw new AppException("All order items must belong to the same restaurant", HttpStatus.BAD_REQUEST);
            }
            SaleItem saleItem = new SaleItem();
            saleItem.setAmount(orderItemRequest.amount());
            saleItem.setSaleItemName(menuItem.getItemName());
            saleItem.setSaleItemPrice(RestaurantUtil.getMenuItemPrice(menuItem.getItemPrice()));
            saleItem.setMenuItem(menuItem);
            saleItem.setRestaurantId(menuItem.getRestaurantId());
            stockRequestList.add(new StockRequest(menuItem.getItemStock().getSku(), saleItem.getAmount()));
            saleItem.setOrder(order);
            saleItemList.add(saleItem);
        }
        stockService.checkStockAvailable(stockRequestList);
        order.setOrderItemList(saleItemList);
        order.setRestaurantId(orderRestaurantId);

        List<TaxRate> allActiveTaxs = taxService.getActiveTaxRates();
        if (allActiveTaxs != null && !allActiveTaxs.isEmpty()) {
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
            order.setOrderTaxes(orderTaxes);
        }
        Double totalPrice = OrderUtil.getTotalPrice(order);
        if (totalPrice <= 0) {
            throw new AppException("Unable to calculate the price", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        order.setTotalPrice(totalPrice);
        if (initiateOrderRequest.getPaymentType() == null) {
            initiateOrderRequest.setPaymentType(PaymentType.CASH);
        }
        order.setPaymentType(initiateOrderRequest.getPaymentType());
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
        order.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        order.setPaymentType(paymentType);
        Order savedOrder = orderRepository.save(order);
        List<ItemStock> itemStocks = new ArrayList<>();
        for (SaleItem saleItem : savedOrder.getOrderItemList()) {
            MenuItem menuItem = saleItem.getMenuItem();
            if (menuItem != null) {
                ItemStock itemStock = menuItem.getItemStock();
                itemStock.setTotalStock(itemStock.getTotalStock() - saleItem.getAmount());
                if (itemStock.getTotalStock() == 0) {
                    menuItem.setIsAvailable(false);
                    menuItemRepository.save(menuItem);
                }
                itemStocks.add(itemStock);
            }
        }
        stockRepository.saveAll(itemStocks);
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

    private MenuItem getAccessibleMenuItem(Long menuItemId) {
        Specification<MenuItem> specification = Specification.where(MenuItemSpecification.hasId(menuItemId))
                .and(MenuItemSpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            specification = specification.and(
                    MenuItemSpecification.belongsToRestaurants(tenantAccessService.resolveAccessibleRestaurantIds(null, null))
            );
        }
        return menuItemRepository.findOne(specification)
                .orElseThrow(() -> new AppException("Menu Item is not valid", HttpStatus.BAD_REQUEST));
    }

    private Order getAccessibleOrder(String orderId) {
        Order order = orderRepository.findByOrderId(orderId).orElseThrow(OrderException::new);
        if (order.isDeleted()) {
            throw new OrderException("Order not found");
        }
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(order.getRestaurantId());
        }
        return order;
    }
}
