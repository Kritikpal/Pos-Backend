package com.kritik.POS.order.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentRequest {
    private String refundReason;
    private String refundNotes;
}
