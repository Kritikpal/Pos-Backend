package com.kritik.POS.restaurant.models.request;

import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
public record ItemRequest(
        Long itemId,
        @NotBlank(message = "Item name is required") String itemName,
        @NotBlank(message = "Item description is required") String description,
        @NotNull(message = "Item price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Item price must be 0 or greater")
        Double itemPrice,
        @NotNull(message = "Category Id is required") Long categoryId,
        Double disCount,
        Boolean isActive,
        Boolean isAvailable,
        Boolean isTrending,
        @Positive(message = "Recipe batch size must be greater than 0")
        Integer recipeBatchSize,
        List<@Valid IngredientUsageRequest> ingredients
) {

    public record IngredientUsageRequest(
            @NotBlank(message = "Ingredient sku is required")
            String ingredientSku,
            @NotNull(message = "Ingredient quantity is required")
            @DecimalMin(value = "0.0001", inclusive = true, message = "Ingredient quantity must be greater than 0")
            Double quantityRequired
    ) {
    }

    public boolean hasIngredientRecipe() {
        return ingredients != null && !ingredients.isEmpty();
    }

    public MenuItem createMenuItemFromRequest(MenuItem menuItem, Category category) {
        menuItem.setCategory(category);
        menuItem.setDescription(this.description().trim());
        menuItem.setItemName(this.itemName().trim());
        ItemPrice itemPrice = menuItem.getItemPrice() == null ? new ItemPrice() : menuItem.getItemPrice();
        itemPrice.setPrice(this.itemPrice());
        menuItem.setItemPrice(itemPrice);
        if (this.disCount() != null) {
            itemPrice.setDisCount(this.disCount());
        } else {
            itemPrice.setDisCount(null);
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
        return menuItem;
    }

}


