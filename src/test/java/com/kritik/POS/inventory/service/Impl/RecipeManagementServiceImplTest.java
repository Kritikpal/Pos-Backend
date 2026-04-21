package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.projection.RecipeMenuItemSearchProjection;
import com.kritik.POS.inventory.models.request.RecipeManagementRequest;
import com.kritik.POS.inventory.models.response.RecipeMenuItemSearchDto;
import com.kritik.POS.inventory.models.response.RecipeManagementResponseDto;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.MenuRecipeRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeManagementServiceImplTest {

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private MenuItemIngredientRepository menuItemIngredientRepository;

    @Mock
    private MenuRecipeRepository menuRecipeRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @InjectMocks
    private RecipeManagementServiceImpl recipeManagementService;

    @Test
    void createRecipeReloadsUsingMenuItemIdAfterSave() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(11L);
        menuItem.setRestaurantId(101L);
        menuItem.setItemName("Paneer Tikka");
        menuItem.setMenuType(MenuType.RECIPE);
        menuItem.setIngredientUsages(new ArrayList<>());

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku("ING-1");
        ingredientStock.setIngredientName("Paneer");
        ingredientStock.setUnitOfMeasure("kg");
        ingredientStock.setTotalStock(12.0);
        ingredientStock.setRestaurantId(101L);

        when(inventoryService.getAccessibleMenuItem(11L)).thenReturn(menuItem);
        when(ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(any())).thenReturn(List.of(ingredientStock));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> {
            MenuItem savedMenuItem = invocation.getArgument(0);
            savedMenuItem.getRecipe().setId(99L);
            return savedMenuItem;
        });
        when(menuRecipeRepository.findByMenuItemId(11L)).thenAnswer(invocation ->
                Optional.of(menuItem.getRecipe())
        );
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);

        RecipeManagementRequest request = new RecipeManagementRequest(
                11L,
                4,
                true,
                List.of(new RecipeManagementRequest.IngredientUsageRequest("ING-1", 2.0))
        );

        RecipeManagementResponseDto response = recipeManagementService.createRecipe(request);

        assertThat(response.menuItemId()).isEqualTo(11L);
        assertThat(response.id()).isEqualTo(99L);
        verify(menuRecipeRepository).findByMenuItemId(11L);
        verify(menuRecipeRepository, never()).findDetailedById(99L);
    }

    @Test
    void recipeEntitiesSupportToStringAndHashCodeWithoutRecursiveOverflow() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(11L);
        menuItem.setItemName("Paneer Tikka");
        menuItem.setIngredientUsages(new ArrayList<>());

        MenuRecipe recipe = new MenuRecipe();
        recipe.setId(99L);
        recipe.setMenuItem(menuItem);
        recipe.setIngredientUsages(new ArrayList<>());

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku("ING-1");
        ingredientStock.setIngredientName("Paneer");
        ingredientStock.setMenuItemIngredients(new ArrayList<>());

        MenuItemIngredient ingredientUsage = new MenuItemIngredient();
        ingredientUsage.setId(501L);
        ingredientUsage.setMenuItem(menuItem);
        ingredientUsage.setRecipe(recipe);
        ingredientUsage.setIngredientStock(ingredientStock);
        ingredientUsage.setQuantityRequired(2.0);

        menuItem.setRecipe(recipe);
        menuItem.getIngredientUsages().add(ingredientUsage);
        recipe.getIngredientUsages().add(ingredientUsage);
        ingredientStock.getMenuItemIngredients().add(ingredientUsage);

        assertThatCode(menuItem::toString).doesNotThrowAnyException();
        assertThatCode(recipe::toString).doesNotThrowAnyException();
        assertThatCode(ingredientUsage::toString).doesNotThrowAnyException();
        assertThatCode(menuItem::hashCode).doesNotThrowAnyException();
        assertThatCode(recipe::hashCode).doesNotThrowAnyException();
        assertThatCode(ingredientUsage::hashCode).doesNotThrowAnyException();
    }

    @Test
    void searchMenuItemsIncludesPreparedMenusInRecipeBasedLookup() {
        List<Long> accessibleRestaurantIds = List.of(101L);
        RecipeMenuItemSearchProjection projection = projection(11L, 77L, "Paneer Tikka", "/images/paneer.png");

        when(tenantAccessService.resolveAccessibleRestaurantIds(1L, 101L)).thenReturn(accessibleRestaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)).thenReturn(accessibleRestaurantIds);
        when(menuItemRepository.searchRecipeMenuItems(anyBoolean(), anyCollection(), anyCollection(), anyString(), any(PageRequest.class)))
                .thenReturn(List.of(projection));

        List<RecipeMenuItemSearchDto> response = recipeManagementService.searchMenuItems(1L, 101L, "paneer", true, 5);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).recipeId()).isEqualTo(77L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MenuType>> typesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(menuItemRepository).searchRecipeMenuItems(
                anyBoolean(),
                anyCollection(),
                typesCaptor.capture(),
                anyString(),
                any(PageRequest.class)
        );
        assertThat(typesCaptor.getValue()).containsExactlyInAnyOrder(MenuType.RECIPE, MenuType.PREPARED);
    }

    private RecipeMenuItemSearchProjection projection(Long id, Long recipeId, String itemName, String image) {
        RecipeMenuItemSearchProjection projection = org.mockito.Mockito.mock(RecipeMenuItemSearchProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getRecipeId()).thenReturn(recipeId);
        when(projection.getItemName()).thenReturn(itemName);
        when(projection.getProductImage()).thenReturn(image);
        return projection;
    }
}
