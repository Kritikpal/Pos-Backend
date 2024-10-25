package com.kritik.POS.order.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.DAO.enums.PaymentType;
import com.kritik.POS.order.model.response.PaymentProcessingResponse;
import com.kritik.POS.order.model.request.InitiateOrderRequest;
import com.kritik.POS.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.kritik.POS.order.route.PaymentRoute.*;

@RestController
public class PaymentController {
    private final OrderService orderService;

    @Autowired
    public PaymentController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping(INITIATE_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> initiatePayment(@RequestBody @Valid InitiateOrderRequest initiateOrderRequest) {
        PaymentProcessingResponse initiateOrderResponse = orderService.initiateOrder(initiateOrderRequest);
        return ResponseEntity.ok(ApiResponse.SUCCESS(initiateOrderResponse));
    }
    @GetMapping(CANCEL_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> cancelPayment(@PathVariable(name = "id") String orderId) {
        PaymentProcessingResponse canceledTransactionResponse = orderService.cancelTransaction(orderId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(canceledTransactionResponse));
    }

    @PostMapping(COMPLETE_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> processCashPayment(@PathVariable(name = "id") String orderId,
                                                                                     @RequestBody PaymentType paymentType) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.completePayment(orderId, paymentType);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse));
    }


    @PatchMapping(REFUND_PAYMENT)
    public ResponseEntity<ApiResponse<PaymentProcessingResponse>> refundPayment(@PathVariable(name = "id") String orderId) {
        PaymentProcessingResponse completePaymentProcessingResponse = orderService.refundPayment(orderId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(completePaymentProcessingResponse));
    }


}
