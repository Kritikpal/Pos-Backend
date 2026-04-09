package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.projection.IngredientStockListProjection;
import com.kritik.POS.restaurant.models.request.IngredientRequest;
import com.kritik.POS.restaurant.models.response.IngredientResponse;
import jakarta.transaction.Transactional;

public interface IngredientService {
    PageResponse<IngredientResponse> getIngredientPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize);

    PageResponse<IngredientStockListProjection> getIngredientPageV2(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize);

    IngredientResponse getIngredientBySku(String sku);

    @Transactional
    IngredientResponse saveIngredient(IngredientRequest ingredientRequest);

    @Transactional
    boolean deleteIngredient(String sku);
}
