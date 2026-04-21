package com.kritik.POS.restaurant.models.request;

import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MenuUpdateRequest(
        Long itemId,
        @NotBlank(message = "Item name is required") String itemName,
        @NotBlank(message = "Item description is required") String description,
        @NotNull(message = "Item price is required")
        @DecimalMin(value = "0.0", message = "Item price must be 0 or greater")
        BigDecimal itemPrice,
        @NotNull(message = "Category Id is required") Long categoryId,
        BigDecimal disCount,
        Boolean isActive,
        Boolean isAvailable,
        Boolean isTrending,
        MenuType menuType,
        Boolean priceIncludesTax,
        Long taxClassId
) {

    public MenuItem createMenuItemFromRequest(MenuItem menuItem, Category category) {
        menuItem.setCategory(category);
        menuItem.setDescription(this.description().trim());
        menuItem.setItemName(this.itemName().trim());
        menuItem.setTaxClassId(this.taxClassId());
        ItemPrice itemPrice = menuItem.getItemPrice() == null ? new ItemPrice() : menuItem.getItemPrice();
        itemPrice.setPrice(this.itemPrice());
        menuItem.setItemPrice(itemPrice);
        if (this.disCount() != null) {
            itemPrice.setDisCount(this.disCount());
        } else {
            itemPrice.setDisCount(null);
        }
        itemPrice.setPriceIncludesTax(Boolean.TRUE.equals(this.priceIncludesTax()));
        if (this.isActive() != null) {
            menuItem.setIsActive(this.isActive());
        }
        if (this.isTrending() != null) {
            menuItem.setIsTrending(this.isTrending());
        }
        if (this.menuType() != null) {
            menuItem.setMenuType(this.menuType());
        }
        if (this.isAvailable() != null) {
            menuItem.setIsAvailable(this.isAvailable());
        }
        return menuItem;
    }

}
