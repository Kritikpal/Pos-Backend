package com.kritik.POS.restaurant.models.request;

import com.kritik.POS.restaurant.DAO.Category;
import com.kritik.POS.restaurant.DAO.ItemPrice;
import com.kritik.POS.restaurant.DAO.ItemStock;
import com.kritik.POS.restaurant.DAO.MenuItem;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;
import java.util.UUID;

@Validated
public record ItemRequest(
        Long itemId,
        @NotNull(message = "Item name is required") String itemName,
        @NotNull(message = "Item description is required") String description,
        @NotNull(message = "Item price is required") Double itemPrice,
        @NotNull(message = "Category Id is required") Long categoryId,
        Double disCount,
        Boolean isActive,
        Boolean isAvailable,
        Boolean isTrending,
        Integer totalStocks
) {

    public MenuItem createMenuItemFromRequest(MenuItem menuItem, Category category) {
        menuItem.setCategory(category);
        menuItem.setDescription(this.description());
        menuItem.setItemName(this.itemName());
        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(this.itemPrice());
        menuItem.setItemPrice(itemPrice);
        if (this.disCount() != null) {
            itemPrice.setDisCount(this.disCount());
        }
        if (this.isActive() != null) {
            menuItem.setIsActive(this.isActive());
        }
        if (this.isTrending() != null) {
            menuItem.setIsTrending(this.isTrending());
        }
        if (this.isAvailable() != null) {
            menuItem.setIsAvailable(this.isAvailable());
        }
        if (menuItem.getItemStock() != null) {
            menuItem.getItemStock().setTotalStock(Objects.requireNonNullElse(this.totalStocks, 100));
        } else {
            ItemStock itemStock = new ItemStock();
            itemStock.setSku(UUID.randomUUID().toString());
            itemStock.setTotalStock(totalStocks);
            itemStock.setMenuItem(menuItem);
            menuItem.setItemStock(itemStock);
        }
        return menuItem;
    }

}


