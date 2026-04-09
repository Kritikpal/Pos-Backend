package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.mapper.RestaurantDtoMapper;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.repository.TaxRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantTableRepository tableRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private RestaurantDtoMapper restaurantDtoMapper;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Test
    void addEditMenuItemBulkLoadsIngredientsAndUpdatesRecipe() {
        Long restaurantId = 11L;
        Long menuItemId = 55L;
        List<Long> accessibleRestaurantIds = List.of(restaurantId);

        Category category = new Category();
        category.setCategoryId(3L);
        category.setCategoryName("Mains");
        category.setCategoryDescription("Main course");
        category.setRestaurantId(restaurantId);

        MenuItem existingMenuItem = buildExistingMenuItem(menuItemId, category, restaurantId);

        IngredientStock bun = buildIngredient("ING-1", "Bun", restaurantId, 20.0, "pcs");
        IngredientStock patty = buildIngredient("ING-2", "Patty", restaurantId, 10.0, "pcs");

        ItemRequest itemRequest = new ItemRequest(
                menuItemId,
                "Burger",
                "Classic burger",
                199.0,
                category.getCategoryId(),
                5.0,
                true,
                true,
                false,
                true,
                4,
                List.of(
                        new ItemRequest.IngredientUsageRequest("ING-1", 2.0),
                        new ItemRequest.IngredientUsageRequest("ING-2", 1.0)
                )
        );

        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(accessibleRestaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)).thenReturn(accessibleRestaurantIds);
        when(menuItemRepository.findDetailedById(menuItemId, false, accessibleRestaurantIds)).thenReturn(Optional.of(existingMenuItem));
        when(categoryRepository.findOne(any(Specification.class))).thenReturn(Optional.of(category));
        when(ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(any())).thenReturn(List.of(bun, patty));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuResponse response = restaurantService.addEditMenuItem(itemRequest, null);

        assertThat(response.getRecipeBased()).isTrue();
        assertThat(response.getIsPrepared()).isTrue();
        assertThat(response.getBatchSize()).isEqualTo(4);
        assertThat(response.getIngredients()).hasSize(2);
        assertThat(existingMenuItem.getRecipe()).isNotNull();
        assertThat(existingMenuItem.getRecipe().getIngredientUsages()).hasSize(2);
        assertThat(existingMenuItem.getIngredientUsages()).hasSize(2);
        assertThat(existingMenuItem.getItemStock().getTotalStock()).isZero();
        assertThat(existingMenuItem.getItemStock().getIsActive()).isFalse();

        verify(ingredientStockRepository).findAllBySkuInAndIsDeletedFalse(any());
        verify(ingredientStockRepository, never()).findBySkuAndIsDeletedFalse(any());
    }

    private MenuItem buildExistingMenuItem(Long menuItemId, Category category, Long restaurantId) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(menuItemId);
        menuItem.setCategory(category);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setItemName("Burger");
        menuItem.setDescription("Old burger");
        menuItem.setIsActive(true);
        menuItem.setIsAvailable(true);
        menuItem.setIsDeleted(false);
        menuItem.setIsTrending(false);

        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(149.0);
        menuItem.setItemPrice(itemPrice);

        ItemStock itemStock = new ItemStock();
        itemStock.setSku("MENU-1");
        itemStock.setTotalStock(9);
        itemStock.setReorderLevel(2);
        itemStock.setUnitOfMeasure("pcs");
        itemStock.setIsActive(true);
        menuItem.setItemStock(itemStock);

        IngredientStock oldIngredient = buildIngredient("OLD-1", "Old ingredient", restaurantId, 5.0, "pcs");
        MenuItemIngredient oldUsage = new MenuItemIngredient();
        oldUsage.setMenuItem(menuItem);
        oldUsage.setIngredientStock(oldIngredient);
        oldUsage.setQuantityRequired(1.0);

        MenuRecipe recipe = new MenuRecipe();
        recipe.setMenuItem(menuItem);
        recipe.setBatchSize(2);
        recipe.setActive(true);
        oldUsage.setRecipe(recipe);
        recipe.setIngredientUsages(new ArrayList<>(List.of(oldUsage)));

        menuItem.setRecipe(recipe);
        menuItem.setHasRecipe(true);
        menuItem.setIngredientUsages(new ArrayList<>(List.of(oldUsage)));
        return menuItem;
    }

    private IngredientStock buildIngredient(String sku, String name, Long restaurantId, Double totalStock, String unit) {
        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku(sku);
        ingredientStock.setIngredientName(name);
        ingredientStock.setRestaurantId(restaurantId);
        ingredientStock.setTotalStock(totalStock);
        ingredientStock.setReorderLevel(2.0);
        ingredientStock.setUnitOfMeasure(unit);
        ingredientStock.setIsActive(true);
        ingredientStock.setIsDeleted(false);
        return ingredientStock;
    }
}
