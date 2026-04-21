package com.kritik.POS.order.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.OrderV2InitiateRequest;
import com.kritik.POS.order.model.request.OrderV2UpdateRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.OrderV2Response;
import com.kritik.POS.order.route.OrderV2Route;
import com.kritik.POS.order.service.OrderV2Service;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrderV2Controller {

    private final OrderV2Service orderV2Service;

    @Tag(name = SwaggerTags.ORDER_V2)
    @PostMapping(OrderV2Route.INITIATE)
    public ResponseEntity<ApiResponse<OrderV2Response>> initiateOrder(
            @RequestBody @Valid OrderV2InitiateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                orderV2Service.initiateOrder(request),
                "Order initiated successfully"
        ));
    }

    @Tag(name = SwaggerTags.ORDER_V2)
    @PatchMapping(OrderV2Route.UPDATE)
    public ResponseEntity<ApiResponse<OrderV2Response>> updateOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid OrderV2UpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                orderV2Service.updateOrder(orderId, request),
                "Order updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.ORDER_V2)
    @GetMapping(OrderV2Route.CANCEL)
    public ResponseEntity<ApiResponse<OrderV2Response>> cancelOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                orderV2Service.cancelTransaction(orderId),
                "Order cancelled successfully"
        ));
    }

    @Tag(name = SwaggerTags.ORDER_V2)
    @PostMapping(OrderV2Route.COMPLETE)
    public ResponseEntity<ApiResponse<OrderV2Response>> completeOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid CompletePaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                orderV2Service.completePayment(orderId, request),
                "Payment completed successfully"
        ));
    }

    @Tag(name = SwaggerTags.ORDER_V2)
    @PatchMapping(OrderV2Route.REFUND)
    public ResponseEntity<ApiResponse<OrderV2Response>> refundOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody(required = false) RefundPaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                orderV2Service.refundPayment(orderId, request),
                "Payment refunded successfully"
        ));
    }
}
