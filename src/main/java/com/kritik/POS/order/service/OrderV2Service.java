package com.kritik.POS.order.service;

import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.OrderV2InitiateRequest;
import com.kritik.POS.order.model.request.OrderV2UpdateRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.OrderV2Response;

public interface OrderV2Service {
    OrderV2Response initiateOrder(OrderV2InitiateRequest request);
    OrderV2Response updateOrder(String orderId, OrderV2UpdateRequest request);
    OrderV2Response cancelTransaction(String orderId);
    OrderV2Response completePayment(String orderId, CompletePaymentRequest request);
    OrderV2Response refundPayment(String orderId, RefundPaymentRequest request);
}
