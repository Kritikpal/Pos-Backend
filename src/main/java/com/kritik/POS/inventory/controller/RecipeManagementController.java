package com.kritik.POS.inventory.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.inventory.models.request.RecipeManagementRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.RecipeMenuItemSearchDto;
import com.kritik.POS.inventory.models.response.RecipeManagementResponseDto;
import com.kritik.POS.inventory.route.InventoryRoute;
import com.kritik.POS.inventory.service.RecipeManagementService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(InventoryRoute.BASE)
public class RecipeManagementController {

    private final RecipeManagementService recipeManagementService;

    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.MENU_INGREDIENT_MAPPING)
    public ResponseEntity<ApiResponse<List<MenuItemIngredientDto>>> menuIngredients(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                recipeManagementService.getIngredientMenuMapping(chainId, restaurantId)
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.SEARCH_RECIPE_MENU_ITEMS)
    public ResponseEntity<ApiResponse<List<RecipeMenuItemSearchDto>>> searchMenuItems(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false) Boolean recipeBased,
            @RequestParam(required = false, defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                recipeManagementService.searchMenuItems(chainId, restaurantId, search, recipeBased, limit)
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.GET_RECIPE)
    public ResponseEntity<ApiResponse<RecipeManagementResponseDto>> getRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(recipeManagementService.getRecipe(id)));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @PostMapping(InventoryRoute.SAVE_RECIPE)
    public ResponseEntity<ApiResponse<RecipeManagementResponseDto>> createRecipe(
            @RequestBody @Valid RecipeManagementRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                recipeManagementService.createRecipe(request),
                "Recipe created successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @PutMapping(InventoryRoute.UPDATE_RECIPE)
    public ResponseEntity<ApiResponse<RecipeManagementResponseDto>> updateRecipe(
            @PathVariable Long id,
            @RequestBody @Valid RecipeManagementRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                recipeManagementService.updateRecipe(id, request),
                "Recipe updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @DeleteMapping(InventoryRoute.DELETE_RECIPE)
    public ResponseEntity<ApiResponse<Boolean>> deleteRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                recipeManagementService.deleteRecipe(id),
                "Recipe deleted successfully"
        ));
    }
}
