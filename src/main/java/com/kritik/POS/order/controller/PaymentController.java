package com.kritik.POS.order.controller;

import static com.kritik.POS.order.route.PaymentRoute.CANCEL_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.COMPLETE_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.INITIATE_PAYMENT;
import static com.kritik.POS.order.route.PaymentRoute.REFUND_PAYMENT;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
            @RequestBody @NotNull(message = "payment type is required") PaymentType paymentType
    ) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.completePayment(orderId, paymentType);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse, "Payment completed successfully"));
    }

    @PatchMapping(REFUND_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> refundPayment(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId
    ) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.refundPayment(orderId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse, "Payment refunded successfully"));
    }
}
