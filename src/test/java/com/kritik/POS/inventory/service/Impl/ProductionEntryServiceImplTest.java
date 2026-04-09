package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.entity.production.ProductionEntry;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.models.request.ProductionEntryCreateRequest;
import com.kritik.POS.inventory.models.response.ProductionEntryResponseDto;
import com.kritik.POS.inventory.projection.ProductionEntrySummaryProjection;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.repository.ProductionEntryItemRepository;
import com.kritik.POS.inventory.repository.ProductionEntryRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionEntryServiceImplTest {

    @Mock
    private ProductionEntryRepository productionEntryRepository;

    @Mock
    private ProductionEntryItemRepository productionEntryItemRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private PreparedItemStockRepository preparedItemStockRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private ProductionEntryServiceImpl productionEntryService;

    @Test
    void createProductionEntryConsumesIngredientsAndIncreasesPreparedStock() {
        MenuItem preparedMenu = buildPreparedMenu();
        ProductionEntry savedEntry = new ProductionEntry();
        savedEntry.setId(99L);
        savedEntry.setRestaurantId(10L);
        savedEntry.setMenuItemId(200L);
        savedEntry.setProducedQty(8.0);
        savedEntry.setUnitCode("PCS");
        savedEntry.setProductionTime(LocalDateTime.of(2026, 4, 6, 12, 0));
        savedEntry.setCreatedBy(7L);
        savedEntry.setCreatedAt(LocalDateTime.of(2026, 4, 6, 12, 0));

        when(inventoryService.getAccessibleMenuItem(200L)).thenReturn(preparedMenu);
        when(tenantAccessService.resolveManageableRestaurantId(10L)).thenReturn(10L);
        when(tenantAccessService.currentUser()).thenReturn(new SecurityUser(
                7L, "chef@example.com", "token", "token-id", 10L, 1L, Set.of("RESTAURANT_ADMIN")
        ));
        when(productionEntryRepository.save(any(ProductionEntry.class))).thenReturn(savedEntry);
        when(ingredientStockRepository.deductStockQuantityIfAvailable(anyString(), anyDouble(), any(LocalDateTime.class))).thenReturn(1);
        when(preparedItemStockRepository.findById(200L)).thenReturn(Optional.empty());
        when(preparedItemStockRepository.save(any(PreparedItemStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductionEntryResponseDto response = productionEntryService.createProductionEntry(
                new ProductionEntryCreateRequest(10L, 200L, 8.0, "PCS", LocalDateTime.of(2026, 4, 6, 12, 0), "Lunch prep")
        );

        verify(inventoryService).checkOrderStockAvailability(org.mockito.ArgumentMatchers.eq(List.of()), org.mockito.ArgumentMatchers.eq(Map.of("ING-10", 4.0)), org.mockito.ArgumentMatchers.eq(Map.<Long, Double>of()));
        verify(ingredientStockRepository).deductStockQuantityIfAvailable(org.mockito.ArgumentMatchers.eq("ING-10"), org.mockito.ArgumentMatchers.eq(4.0), any(LocalDateTime.class));
        verify(productionEntryItemRepository).saveAll(anyCollection());
        verify(inventoryService).refreshMenuAvailability(org.mockito.ArgumentMatchers.eq(List.of(200L)), org.mockito.ArgumentMatchers.eq(Set.of("ING-10")));
        assertThat(response.menuItemId()).isEqualTo(200L);
        assertThat(response.availablePreparedQty()).isEqualTo(8.0);
        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).ingredientSku()).isEqualTo("ING-10");
    }

    @Test
    void getProductionEntryPageMapsProjectionRows() {
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, 10L)).thenReturn(List.of(10L));
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(List.of(10L))).thenReturn(List.of(10L));
        when(productionEntryRepository.findSummaries(false, List.of(10L), 200L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(summaryProjection())));

        PageResponse<com.kritik.POS.inventory.models.response.ProductionEntrySummaryDto> page = productionEntryService.getProductionEntryPage(
                null,
                10L,
                200L,
                0,
                20
        );

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).menuItemName()).isEqualTo("Paneer Roll");
    }

    private MenuItem buildPreparedMenu() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(200L);
        menuItem.setRestaurantId(10L);
        menuItem.setItemName("Paneer Roll");
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);
        menuItem.setHasRecipe(true);
        menuItem.setIsPrepared(true);

        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(120.0);
        menuItem.setItemPrice(itemPrice);

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku("ING-10");
        ingredientStock.setIngredientName("Paneer");
        ingredientStock.setUnitOfMeasure("KG");
        ingredientStock.setRestaurantId(10L);
        ingredientStock.setIsActive(true);
        ingredientStock.setIsDeleted(false);
        ingredientStock.setTotalStock(20.0);

        MenuRecipe recipe = new MenuRecipe();
        recipe.setMenuItem(menuItem);
        recipe.setBatchSize(10);

        MenuItemIngredient ingredientUsage = new MenuItemIngredient();
        ingredientUsage.setMenuItem(menuItem);
        ingredientUsage.setRecipe(recipe);
        ingredientUsage.setIngredientStock(ingredientStock);
        ingredientUsage.setQuantityRequired(5.0);

        recipe.setIngredientUsages(List.of(ingredientUsage));
        menuItem.setRecipe(recipe);
        menuItem.setIngredientUsages(List.of(ingredientUsage));
        return menuItem;
    }

    private ProductionEntrySummaryProjection summaryProjection() {
        return new ProductionEntrySummaryProjection() {
            @Override
            public Long getId() {
                return 99L;
            }

            @Override
            public Long getRestaurantId() {
                return 10L;
            }

            @Override
            public Long getMenuItemId() {
                return 200L;
            }

            @Override
            public String getMenuItemName() {
                return "Paneer Roll";
            }

            @Override
            public Double getProducedQty() {
                return 8.0;
            }

            @Override
            public String getUnitCode() {
                return "PCS";
            }

            @Override
            public LocalDateTime getProductionTime() {
                return LocalDateTime.of(2026, 4, 6, 12, 0);
            }

            @Override
            public Long getCreatedBy() {
                return 7L;
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return LocalDateTime.of(2026, 4, 6, 12, 0);
            }
        };
    }
}
