package com.kritik.POS.order.service;

import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderUpdateRequest;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;

public interface ConfiguredOrderService {

    ConfiguredOrderResponse initiateOrder(ConfiguredOrderInitiateRequest request);

    ConfiguredOrderResponse updateOrder(String orderId, ConfiguredOrderUpdateRequest request);

    ConfiguredOrderResponse cancelTransaction(String orderId);

    ConfiguredOrderResponse completePayment(String orderId, CompletePaymentRequest request);

    ConfiguredOrderResponse refundPayment(String orderId, RefundPaymentRequest request);
}
