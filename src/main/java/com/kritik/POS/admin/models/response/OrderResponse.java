package com.kritik.POS.admin.models.response;

import com.kritik.POS.order.DAO.Order;
import com.kritik.POS.order.DAO.enums.PaymentStatus;
import com.kritik.POS.order.DAO.enums.PaymentType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class OrderResponse {
    private String orderId;
    private Double totalPrice;
    private PaymentType paymentType;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentTime = LocalDateTime.now();
    private List<OrderItemResponse> orderItemList = new ArrayList<>();
    @Data
    static class OrderItemResponse {
        private String saleItemName;
        private Double saleItemPrice;
        private Integer amount;
    }

    public static OrderResponse buildObjectFromOrder(Order order) {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setPaymentStatus(order.getPaymentStatus());
        orderResponse.setPaymentType(order.getPaymentType());
        orderResponse.setPaymentTime(order.getLastUpdatedTime());
        orderResponse.setOrderId(order.getOrderId());
        orderResponse.setTotalPrice(order.getTotalPrice());
        List<OrderResponse.OrderItemResponse> list = order.getOrderItemList().stream().map(saleItem -> {
            OrderResponse.OrderItemResponse orderItemResponse = new OrderResponse.OrderItemResponse();
            orderItemResponse.setAmount(saleItem.getAmount());
            orderItemResponse.setSaleItemName(saleItem.getSaleItemName());
            orderItemResponse.setSaleItemPrice(saleItem.getSaleItemPrice());
            return orderItemResponse;
        }).toList();
        orderResponse.getOrderItemList().addAll(list);
        return orderResponse;
    }

}
