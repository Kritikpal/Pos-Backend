package com.kritik.POS.order.model.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.kritik.POS.order.entity.enums.PaymentType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompletePaymentRequest {

    @NotNull(message = "payment type is required")
    private PaymentType paymentType;
    private String paymentReference;
    private String paymentCollectedBy;
    private String paymentNotes;
    private String externalTxnId;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static CompletePaymentRequest fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return new CompletePaymentRequest();
        }
        if (node.isTextual()) {
            return new CompletePaymentRequest(PaymentType.valueOf(node.asText()), null, null, null, null);
        }
        return new CompletePaymentRequest(
                node.hasNonNull("paymentType") ? PaymentType.valueOf(node.get("paymentType").asText()) : null,
                node.hasNonNull("paymentReference") ? node.get("paymentReference").asText() : null,
                node.hasNonNull("paymentCollectedBy") ? node.get("paymentCollectedBy").asText() : null,
                node.hasNonNull("paymentNotes") ? node.get("paymentNotes").asText() : null,
                node.hasNonNull("externalTxnId") ? node.get("externalTxnId").asText() : null
        );
    }
}
