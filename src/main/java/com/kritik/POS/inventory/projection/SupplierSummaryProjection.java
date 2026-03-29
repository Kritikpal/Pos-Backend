package com.kritik.POS.inventory.projection;

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
