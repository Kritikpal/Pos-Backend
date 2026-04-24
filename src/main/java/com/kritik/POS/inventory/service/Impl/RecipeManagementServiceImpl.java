package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.models.request.RecipeManagementRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.RecipeMenuItemSearchDto;
import com.kritik.POS.inventory.models.response.RecipeManagementResponseDto;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.MenuRecipeRepository;
import com.kritik.POS.inventory.service.RecipeManagementService;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipeManagementServiceImpl implements RecipeManagementService {

    private final InventoryService inventoryService;
    private final TenantAccessService tenantAccessService;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final MenuRecipeRepository menuRecipeRepository;
    private final MenuItemRepository menuItemRepository;
    private final IngredientStockRepository ingredientStockRepository;

    @Override
    public List<MenuItemIngredientDto> getIngredientMenuMapping(Long chainId, Long restaurantId) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return menuItemIngredientRepository.findAllForRestaurant(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
        ).stream().map(MenuItemIngredientDto::fromProjection).toList();
    }

    @Override
    public List<RecipeMenuItemSearchDto> searchMenuItems(Long chainId,
                                                         Long restaurantId,
                                                         String search,
                                                         Boolean recipeBased,
                                                         Integer limit) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }

        int pageSize = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        return menuItemRepository.searchRecipeMenuItems(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        resolveRecipeSearchTypes(),
                        normalizeSearch(search),
                        PageRequest.of(0, pageSize)
                ).stream()
                .map(RecipeMenuItemSearchDto::fromProjection)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeManagementResponseDto getRecipe(Long id) {
        return RecipeManagementResponseDto.fromEntity(getAccessibleRecipeById(id));
    }

    @Override
    @Transactional
    public RecipeManagementResponseDto createRecipe(RecipeManagementRequest request) {
        MenuItem menuItem = inventoryService.getAccessibleMenuItem(request.menuItemId());
        if (menuItem.getMenuType() != MenuType.RECIPE && menuItem.getMenuType() != MenuType.PREPARED) {
            throw new AppException("Only recipe or prepared menu items can have a recipe", HttpStatus.BAD_REQUEST);
        }
        if (menuItem.getRecipe() != null) {
            throw new AppException("Recipe already exists for selected menu item", HttpStatus.BAD_REQUEST);
        }
        MenuRecipe recipe = new MenuRecipe();
        return saveRecipe(menuItem, recipe, request, false);
    }

    @Override
    @Transactional
    public RecipeManagementResponseDto updateRecipe(Long id, RecipeManagementRequest request) {
        MenuRecipe recipe = getAccessibleRecipeById(id);
        if (!recipe.getMenuItem().getId().equals(request.menuItemId())) {
            throw new AppException("Recipe menu item cannot be changed", HttpStatus.BAD_REQUEST);
        }
        return saveRecipe(recipe.getMenuItem(), recipe, request, true);
    }

    @Override
    @Transactional
    public boolean deleteRecipe(Long id) {
        MenuRecipe recipe = getAccessibleRecipeById(id);
        MenuItem menuItem = recipe.getMenuItem();
        if (menuItem.getMenuType() == MenuType.PREPARED) {
            throw new AppException("Prepared menu items must also have a recipe", HttpStatus.BAD_REQUEST);
        }

        Set<String> affectedIngredientSkus = new LinkedHashSet<>();
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            affectedIngredientSkus.add(ingredientUsage.getIngredientStock().getSku());
        }

        menuItem.getIngredientUsages().clear();
        recipe.getIngredientUsages().clear();
        menuItem.setRecipe(null);
        menuItemRepository.save(menuItem);

        inventoryService.refreshMenuAvailability(List.of(menuItem.getId()), affectedIngredientSkus);
        return true;
    }

    private RecipeManagementResponseDto saveRecipe(MenuItem menuItem,
                                                   MenuRecipe recipe,
                                                   RecipeManagementRequest request,
                                                   boolean preserveExistingActive) {
        Set<String> affectedIngredientSkus = new LinkedHashSet<>();
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            affectedIngredientSkus.add(ingredientUsage.getIngredientStock().getSku());
        }

        Map<String, MenuItemIngredient> existingUsages = new LinkedHashMap<>();
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            existingUsages.put(ingredientUsage.getIngredientStock().getSku(), ingredientUsage);
        }

        Map<String, IngredientStock> ingredientStocksBySku = loadIngredientStocksBySku(
                menuItem.getRestaurantId(),
                request.ingredients()
        );
        affectedIngredientSkus.addAll(ingredientStocksBySku.keySet());

        recipe.setMenuItem(menuItem);
        recipe.setBatchSize(request.batchSize());
        recipe.setActive(resolveRecipeActiveFlag(recipe, request.active(), preserveExistingActive));

        Set<String> requestedSkus = new LinkedHashSet<>();
        List<MenuItemIngredient> nextUsages = new ArrayList<>();
        for (RecipeManagementRequest.IngredientUsageRequest ingredientRequest : request.ingredients()) {
            if (!requestedSkus.add(ingredientRequest.ingredientSku())) {
                continue;
            }

            MenuItemIngredient ingredientUsage = existingUsages.getOrDefault(
                    ingredientRequest.ingredientSku(),
                    new MenuItemIngredient()
            );
            ingredientUsage.setMenuItem(menuItem);
            ingredientUsage.setRecipe(recipe);
            ingredientUsage.setIngredientStock(ingredientStocksBySku.get(ingredientRequest.ingredientSku()));
            ingredientUsage.setQuantityRequired(ingredientRequest.quantityRequired());
            nextUsages.add(ingredientUsage);
        }

        menuItem.getIngredientUsages().clear();
        menuItem.getIngredientUsages().addAll(nextUsages);
        recipe.getIngredientUsages().clear();
        recipe.getIngredientUsages().addAll(nextUsages);
        menuItem.setRecipe(recipe);

        MenuItem savedMenuItem = menuItemRepository.save(menuItem);
        inventoryService.refreshMenuAvailability(List.of(savedMenuItem.getId()), affectedIngredientSkus);
        return RecipeManagementResponseDto.fromEntity(getAccessibleRecipeByMenuItemId(savedMenuItem.getId()));
    }

    private Boolean resolveRecipeActiveFlag(MenuRecipe recipe, Boolean requestedActive, boolean preserveExistingActive) {
        if (requestedActive != null) {
            return requestedActive;
        }
        if (preserveExistingActive && recipe.getId() != null && recipe.getActive() != null) {
            return recipe.getActive();
        }
        return true;
    }

    private MenuRecipe getAccessibleRecipeById(Long id) {
        MenuRecipe recipe = menuRecipeRepository.findDetailedById(id)
                .orElseThrow(() -> new AppException("Recipe not found", HttpStatus.BAD_REQUEST));
        validateRecipeAccess(recipe);
        return recipe;
    }

    private MenuRecipe getAccessibleRecipeByMenuItemId(Long menuItemId) {
        MenuRecipe recipe = menuRecipeRepository.findByMenuItemId(menuItemId)
                .orElseThrow(() -> new AppException("Recipe not found", HttpStatus.BAD_REQUEST));
        validateRecipeAccess(recipe);
        return recipe;
    }

    private void validateRecipeAccess(MenuRecipe recipe) {
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(recipe.getMenuItem().getRestaurantId());
        }
    }

    private Map<String, IngredientStock> loadIngredientStocksBySku(Long restaurantId,
                                                                   List<RecipeManagementRequest.IngredientUsageRequest> ingredientRequests) {
        Set<String> requestedSkus = new LinkedHashSet<>();
        for (RecipeManagementRequest.IngredientUsageRequest ingredientRequest : ingredientRequests) {
            requestedSkus.add(ingredientRequest.ingredientSku());
        }

        Map<String, IngredientStock> ingredientStocksBySku = new LinkedHashMap<>();
        for (IngredientStock ingredientStock : ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(requestedSkus)) {
            if (!restaurantId.equals(ingredientStock.getRestaurantId())) {
                throw new AppException("Ingredient does not belong to the selected restaurant", HttpStatus.BAD_REQUEST);
            }
            ingredientStocksBySku.put(ingredientStock.getSku(), ingredientStock);
        }

        validateAllIngredientsResolved(requestedSkus, ingredientStocksBySku.keySet());
        return ingredientStocksBySku;
    }

    private void validateAllIngredientsResolved(Collection<String> requestedSkus, Collection<String> resolvedSkus) {
        if (requestedSkus.size() == resolvedSkus.size()) {
            return;
        }
        for (String requestedSku : requestedSkus) {
            if (!resolvedSkus.contains(requestedSku)) {
                throw new AppException("Invalid ingredient sku", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String normalizeSearch(String search) {
        return search == null ? null : search.trim();
    }

    private Set<MenuType> resolveRecipeSearchTypes() {
        return Set.of(MenuType.RECIPE, MenuType.PREPARED);
    }
}
