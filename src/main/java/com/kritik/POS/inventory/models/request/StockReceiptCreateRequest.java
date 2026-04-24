package com.kritik.POS.inventory.models.request;

import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record StockReceiptCreateRequest(
        Long restaurantId,
        @NotNull(message = "Supplier id is required")
        Long supplierId,
        @Size(max = 80, message = "Invoice number must be 80 characters or less")
        String invoiceNumber,
        LocalDateTime receivedAt,
        @Size(max = 1000, message = "Notes must be 1000 characters or less")
        String notes,
        @NotEmpty(message = "At least one receipt item is required")
        List<@Valid ReceiptItemRequest> items
    ) {
    public record ReceiptItemRequest(
            @NotBlank(message = "SKU is required")
            String sku,
            StockReceiptSkuType skuType,
            @DecimalMin(value = "0.000001", message = "Entered quantity must be greater than 0")
            Double enteredQty,
            @Min(value = 1, message = "Quantity received must be at least 1")
            Integer quantityReceived,
            Long unitId,
            @NotNull(message = "Unit cost is required")
            @DecimalMin(value = "0.0", message = "Unit cost must be 0 or greater")
            Double unitCost
    ) {
    }
}
