package com.kritik.POS.tax.projection;

import java.time.LocalDateTime;

public interface TaxRateSummaryProjection {
    Long getTaxId();
    Long getRestaurantId();
    String getTaxName();
    Double getTaxAmount();
    boolean getIsActive();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
