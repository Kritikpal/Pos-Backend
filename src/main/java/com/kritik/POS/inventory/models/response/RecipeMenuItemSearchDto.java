package com.kritik.POS.inventory.models.response;

import com.kritik.POS.inventory.projection.RecipeMenuItemSearchProjection;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;

public record RecipeMenuItemSearchDto(
        Long id,
        Long recipeId,
        String name,
        String image
) {
    public static RecipeMenuItemSearchDto fromProjection(RecipeMenuItemSearchProjection projection) {
        return new RecipeMenuItemSearchDto(
                projection.getId(),
                projection.getRecipeId(),
                projection.getItemName(),
                ProductImageUrlUtil.toClientUrl(projection.getProductImage())
        );
    }
}
