package com.kritik.POS.order.service;

import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;

public interface OrderService {
    PaymentProcessingResponse initiateOrder(InitiateOrderRequest initiateOrderRequest);
    PaymentProcessingResponse updateOrder(String orderId, InitiateOrderRequest initiateOrderRequest);
    PaymentProcessingResponse cancelTransaction(String orderId);
    PaymentProcessingResponse completePayment(String orderId, CompletePaymentRequest request);
    PaymentProcessingResponse refundPayment(String orderId, RefundPaymentRequest request);
}
