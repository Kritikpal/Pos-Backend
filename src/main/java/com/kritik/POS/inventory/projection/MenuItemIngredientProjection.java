package com.kritik.POS.inventory.projection;

import java.time.LocalDateTime;

public interface MenuItemIngredientProjection {

    Long getId();

    Long getMenuId();

    Long getRestaurantId();

    String getMenuItemName();

    String getIngredientSku();

    String getIngredientName();

    Double getQuantityRequired();

    Integer getBatchSize();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
