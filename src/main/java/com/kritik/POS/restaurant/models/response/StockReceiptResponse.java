package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.stockEntry.StockReceipt;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockReceiptResponse {
    private Long receiptId;
    private String receiptNumber;
    private Long restaurantId;
    private SupplierResponse supplier;
    private String invoiceNumber;
    private LocalDateTime receivedAt;
    private Integer totalItems;
    private Integer totalQuantity;
    private Double totalCost;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StockReceiptItemResponse> items;

    public static StockReceiptResponse fromEntity(StockReceipt stockReceipt) {
        StockReceiptResponse response = new StockReceiptResponse();
        response.setReceiptId(stockReceipt.getReceiptId());
        response.setReceiptNumber(stockReceipt.getReceiptNumber());
        response.setRestaurantId(stockReceipt.getRestaurantId());
        response.setSupplier(SupplierResponse.fromEntity(stockReceipt.getSupplier()));
        response.setInvoiceNumber(stockReceipt.getInvoiceNumber());
        response.setReceivedAt(stockReceipt.getReceivedAt());
        response.setTotalItems(stockReceipt.getTotalItems());
        response.setTotalQuantity(stockReceipt.getTotalQuantity());
        response.setTotalCost(stockReceipt.getTotalCost());
        response.setNotes(stockReceipt.getNotes());
        response.setCreatedAt(stockReceipt.getCreatedAt());
        response.setUpdatedAt(stockReceipt.getUpdatedAt());
        response.setItems(stockReceipt.getReceiptItems().stream()
                .map(StockReceiptItemResponse::fromEntity)
                .toList());
        return response;
    }
}
