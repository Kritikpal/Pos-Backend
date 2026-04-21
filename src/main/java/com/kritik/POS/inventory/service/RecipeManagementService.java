package com.kritik.POS.inventory.service;

import com.kritik.POS.inventory.models.request.RecipeManagementRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.RecipeMenuItemSearchDto;
import com.kritik.POS.inventory.models.response.RecipeManagementResponseDto;

import java.util.List;

public interface RecipeManagementService {
    List<MenuItemIngredientDto> getIngredientMenuMapping(Long chainId, Long restaurantId);

    List<RecipeMenuItemSearchDto> searchMenuItems(Long chainId, Long restaurantId, String search, Boolean recipeBased, Integer limit);

    RecipeManagementResponseDto getRecipe(Long id);

    RecipeManagementResponseDto createRecipe(RecipeManagementRequest request);

    RecipeManagementResponseDto updateRecipe(Long id, RecipeManagementRequest request);

    boolean deleteRecipe(Long id);
}
