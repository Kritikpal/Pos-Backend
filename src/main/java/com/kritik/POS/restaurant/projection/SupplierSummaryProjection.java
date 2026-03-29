package com.kritik.POS.restaurant.projection;

import java.time.LocalDateTime;

public interface SupplierSummaryProjection {
    Long getSupplierId();
    Long getRestaurantId();
    String getSupplierName();
    String getContactPerson();
    String getPhoneNumber();
    String getEmail();
    Boolean getIsActive();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
