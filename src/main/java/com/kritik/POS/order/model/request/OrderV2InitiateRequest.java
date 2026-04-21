package com.kritik.POS.order.model.request;

import com.kritik.POS.order.entity.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request payload for creating or updating an order cart")
public class OrderV2InitiateRequest {

    @NotEmpty(message = "Add at least one item")
    @NotNull(message = "Add at least one item")
    @Schema(description = "Items added to the cart")
    private List<@Valid OrderItemRequest> orderItems;

    @Schema(description = "Preferred payment type", example = "CASH")
    private PaymentType paymentType;

    @Schema(description = "Optional buyer tax context used for compliant invoices and jurisdiction-sensitive tax rules")
    private OrderTaxContextRequest taxContext;

    public record OrderItemRequest(
            @NotNull(message = "Menu item id is required")
            @Schema(description = "Menu item id to add into the order", example = "12")
            Long menuItemId,
            @NotNull(message = "Amount is required")
            @Min(value = 1, message = "Amount must be at least 1")
            @Schema(description = "Quantity of this menu item", example = "2", minimum = "1")
            Integer amount,
            @Schema(description = "Required only when the selected menu item is configurable")
            @Valid ConfigurationRequest configuration
    ) {
    }

    public record ConfigurationRequest(
            @NotEmpty(message = "At least one slot entry is required")
            @Schema(description = "Quantity items for each configurable slot")
            List<@Valid SlotItemRequest> slotItems
    ) {
    }

    public record SlotItemQuantityRequest(
            @NotNull(message = "Child menu item id is required")
            @Schema(description = "Child menu item id", example = "201")
            Long childMenuItemId,
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Schema(description = "Quantity for this slot item", example = "2", minimum = "1")
            Integer quantity
    ) {
    }

    public record SlotItemRequest(
            @NotNull(message = "Slot id is required")
            @Schema(description = "Configured template slot id", example = "101")
            Long slotId,
            @NotEmpty(message = "At least one slot item is required")
            @Schema(description = "Child menu items with quantities for this slot")
            List<@Valid SlotItemQuantityRequest> items
    ) {
    }
}
