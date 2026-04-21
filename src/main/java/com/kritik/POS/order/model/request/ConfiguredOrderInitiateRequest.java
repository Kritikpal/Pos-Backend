package com.kritik.POS.order.model.request;

import com.kritik.POS.order.entity.enums.PaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ConfiguredOrderInitiateRequest {

    @NotEmpty(message = "Add at least one configured item")
    @NotNull(message = "Add at least one configured item")
    private List<@Valid ConfiguredOrderItemRequest> orderItems;

    private PaymentType paymentType;

    private OrderTaxContextRequest taxContext;

    public record ConfiguredOrderItemRequest(
            @NotNull(message = "Configured menu template id is required")
            Long configuredMenuTemplateId,
            @NotNull(message = "Amount is required")
            @Min(value = 1, message = "Amount must be at least 1")
            Integer amount,
            List<@Valid SlotItemRequest> slotItems
    ) {
    }

    public record SlotItemQuantityRequest(
            @NotNull(message = "Child menu item id is required")
            Long childMenuItemId,
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            Integer quantity
    ) {
    }

    public record SlotItemRequest(
            @NotNull(message = "Slot id is required")
            Long slotId,
            @NotEmpty(message = "At least one slot item is required")
            List<@Valid SlotItemQuantityRequest> items
    ) {
    }
}
