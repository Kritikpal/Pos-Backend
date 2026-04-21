package com.kritik.POS.configuredmenu.models.response;

import com.kritik.POS.inventory.projection.RecipeMenuItemSearchProjection;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;

public record ConfiguredMenuItemSearchDto(
        Long id,
        Long recipeId,
        String itemName,
        String productImage
) {
    public static ConfiguredMenuItemSearchDto fromProjection(RecipeMenuItemSearchProjection projection) {
        return new ConfiguredMenuItemSearchDto(
                projection.getId(),
                projection.getRecipeId(),
                projection.getItemName(),
                ProductImageUrlUtil.toClientUrl(projection.getProductImage())
        );
    }
}
