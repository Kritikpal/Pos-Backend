package com.kritik.POS.order.controller;

import static com.kritik.POS.order.route.PaymentRoute.CANCEL_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.COMPLETE_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.INITIATE_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.REFUND_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.UPDATE_PAYMENT;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class PaymentController {
    private final OrderService orderService;

    @Autowired
    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping(INITIATE_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> initiatePayment(
            @RequestBody @Valid InitiateOrderRequest initiateOrderRequest
    ) {
        PaymentProcessingResponse initiateOrderResponse = orderService.initiateOrder(initiateOrderRequest);
        return ResponseEntity.ok(ApiResponse.SUCCESS(initiateOrderResponse, "Order initiated successfully"));
    }

    @PatchMapping(UPDATE_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> updatePayment(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid InitiateOrderRequest initiateOrderRequest
    ) {
        PaymentProcessingResponse updatedOrderResponse = orderService.updateOrder(orderId, initiateOrderRequest);
        return ResponseEntity.ok(ApiResponse.SUCCESS(updatedOrderResponse, "Order updated successfully"));
    }

    @GetMapping(CANCEL_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> cancelPayment(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId
    ) {
        PaymentProcessingResponse canceledTransactionResponse = orderService.cancelTransaction(orderId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(canceledTransactionResponse, "Payment cancelled successfully"));
    }

    @PostMapping(COMPLETE_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> processCashPayment(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid CompletePaymentRequest request
    ) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.completePayment(orderId, request);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse, "Payment completed successfully"));
    }

    @PatchMapping(REFUND_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> refundPayment(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody(required = false) RefundPaymentRequest request
    ) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.refundPayment(orderId, request);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse, "Payment refunded successfully"));
    }
}
