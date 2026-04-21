package com.kritik.POS.order.model.response;

import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ConfiguredOrderResponse {

    private String orderId;
    private String message;
    private String description;
    private PaymentType paymentType;
    private PaymentStatus paymentStatus;
    private BigDecimal totalPrice;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal taxAmount;
    private BigDecimal feeAmount;
    private BigDecimal roundingAmount;
    private BigDecimal grandTotal;
    private LocalDateTime paymentCompletedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime refundedAt;
    private String paymentReference;
    private String paymentCollectedBy;
    private String paymentNotes;
    private String externalTxnId;
    private Long operatorUserId;
    private Long refundOperatorUserId;
    private String refundReason;
    private String refundNotes;
    private List<ConfiguredSaleItemResponse> items;
    private List<OrderTaxSummaryResponse> taxSummaries;

    public ConfiguredOrderResponse(Order order,
                                   List<ConfiguredSaleItemResponse> items,
                                   String message,
                                   String description) {
        this.message = message;
        this.description = description;
        this.orderId = order.getOrderId();
        this.paymentType = order.getPaymentType();
        this.paymentStatus = order.getPaymentStatus();
        this.totalPrice = order.getTotalPrice();
        this.subtotalAmount = order.getSubtotalAmount();
        this.discountAmount = order.getDiscountAmount();
        this.taxableAmount = order.getTaxableAmount();
        this.taxAmount = order.getTaxAmount();
        this.feeAmount = order.getFeeAmount();
        this.roundingAmount = order.getRoundingAmount();
        this.grandTotal = order.getGrandTotal();
        this.paymentCompletedAt = order.getPaymentCompletedAt();
        this.cancelledAt = order.getCancelledAt();
        this.refundedAt = order.getRefundedAt();
        this.paymentReference = order.getPaymentReference();
        this.paymentCollectedBy = order.getPaymentCollectedBy();
        this.paymentNotes = order.getPaymentNotes();
        this.externalTxnId = order.getExternalTxnId();
        this.operatorUserId = order.getOperatorUserId();
        this.refundOperatorUserId = order.getRefundOperatorUserId();
        this.refundReason = order.getRefundReason();
        this.refundNotes = order.getRefundNotes();
        this.items = items;
        this.taxSummaries = order.getOrderTaxSummaries().stream().map(OrderTaxSummaryResponse::fromEntity).toList();
    }
}
