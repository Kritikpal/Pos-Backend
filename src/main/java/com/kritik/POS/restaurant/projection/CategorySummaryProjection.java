package com.kritik.POS.restaurant.projection;

import java.time.LocalDateTime;

public interface CategorySummaryProjection {
    Long getCategoryId();
    Long getRestaurantId();
    String getCategoryName();
    String getCategoryDescription();
    Boolean getIsActive();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
