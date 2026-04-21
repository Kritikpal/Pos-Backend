package com.kritik.POS.order.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderUpdateRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;
import com.kritik.POS.order.route.ConfiguredOrderRoute;
import com.kritik.POS.order.service.ConfiguredOrderService;
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
public class ConfiguredOrderController {

    private final ConfiguredOrderService configuredOrderService;

    @Tag(name = SwaggerTags.CONFIGURED_ORDER)
    @PostMapping(ConfiguredOrderRoute.INITIATE)
    public ResponseEntity<ApiResponse<ConfiguredOrderResponse>> initiateOrder(
            @RequestBody @Valid ConfiguredOrderInitiateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredOrderService.initiateOrder(request),
                "Configured order initiated successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_ORDER)
    @PatchMapping(ConfiguredOrderRoute.UPDATE)
    public ResponseEntity<ApiResponse<ConfiguredOrderResponse>> updateOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid ConfiguredOrderUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredOrderService.updateOrder(orderId, request),
                "Configured order updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_ORDER)
    @GetMapping(ConfiguredOrderRoute.CANCEL)
    public ResponseEntity<ApiResponse<ConfiguredOrderResponse>> cancelOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredOrderService.cancelTransaction(orderId),
                "Configured order cancelled successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_ORDER)
    @PostMapping(ConfiguredOrderRoute.COMPLETE)
    public ResponseEntity<ApiResponse<ConfiguredOrderResponse>> completeOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody @Valid CompletePaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredOrderService.completePayment(orderId, request),
                "Configured order payment completed successfully"
        ));
    }

    @Tag(name = SwaggerTags.CONFIGURED_ORDER)
    @PatchMapping(ConfiguredOrderRoute.REFUND)
    public ResponseEntity<ApiResponse<ConfiguredOrderResponse>> refundOrder(
            @PathVariable(name = "id") @NotBlank(message = "order id is required") String orderId,
            @RequestBody(required = false) RefundPaymentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                configuredOrderService.refundPayment(orderId, request),
                "Configured order refunded successfully"
        ));
    }
}
