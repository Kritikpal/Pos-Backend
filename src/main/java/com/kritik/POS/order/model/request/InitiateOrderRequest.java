package com.kritik.POS.order.model.request;

import com.kritik.POS.order.entity.enums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class InitiateOrderRequest {

    @NotEmpty(message = "Add at least one item")
    @NotNull(message = "Add at least one item")
    private List<@Valid OrderItemRequest> orderItems;

    private PaymentType paymentType;

    private OrderTaxContextRequest taxContext;

    public record OrderItemRequest(@NotNull Long menuItemId, @NotNull Integer amount) {
    }
}
