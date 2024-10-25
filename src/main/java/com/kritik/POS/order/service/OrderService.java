package com.kritik.POS.order.service;

import com.kritik.POS.order.DAO.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.model.request.InitiateOrderRequest;

public interface OrderService {
    PaymentProcessingResponse initiateOrder(InitiateOrderRequest initiateOrderRequest);
    PaymentProcessingResponse cancelTransaction(String orderId);
    PaymentProcessingResponse completePayment(String orderId, PaymentType passcode);
    PaymentProcessingResponse refundPayment(String orderId);
}
