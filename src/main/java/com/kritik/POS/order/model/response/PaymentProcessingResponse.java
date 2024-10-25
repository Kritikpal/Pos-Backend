package com.kritik.POS.order.model.response;

import com.kritik.POS.order.DAO.Order;
import com.kritik.POS.order.DAO.enums.PaymentStatus;
import com.kritik.POS.order.DAO.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentProcessingResponse {

    private String orderId;
    private String message;
    private String description;
    private PaymentType paymentType;
    private PaymentStatus paymentStatus;
    private Double totalPrice;

    public PaymentProcessingResponse(Order order, String message, String description) {
        this.message = message;
        this.description = description;
        this.orderId = order.getOrderId();
        this.paymentType = order.getPaymentType();
        this.paymentStatus = order.getPaymentStatus();
        this.totalPrice = order.getTotalPrice();
    }


}
