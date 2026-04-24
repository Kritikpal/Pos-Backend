package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.request.StockUpdateRequest;
import com.kritik.POS.inventory.models.response.StockResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.service.ItemUnitConversionService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectStockMetadataServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private MenuItemIngredientRepository menuItemIngredientRepository;

    @Mock
    private PreparedItemStockRepository preparedItemStockRepository;

    @Mock
    private SaleItemRepository saleItemRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private InventoryUtil inventoryUtil;

    @Mock
    private ItemUnitConversionService itemUnitConversionService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Test
    void saveStockCreatesMetadataOnlyStockWithZeroQuantity() {
        MenuItem menuItem = directMenuItem();
        UnitMaster unitMaster = unit("PCS");
        when(tenantAccessService.isSuperAdmin()).thenReturn(true);
        when(menuItemRepository.findOne(any(Specification.class))).thenReturn(Optional.of(menuItem));
        when(itemUnitConversionService.resolveBaseUnit(null, "PCS", "unit")).thenReturn(unitMaster);
        when(stockRepository.save(any(ItemStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockResponse response = inventoryService.saveStock(new ItemStockUpsertRequest(
                10L,
                3,
                null,
                "PCS",
                null,
                null,
                true
        ));

        assertThat(response.getTotalStock()).isEqualTo(0);
        assertThat(response.getReorderLevel()).isEqualTo(3);
        assertThat(response.getUnitOfMeasure()).isEqualTo("PCS");
    }

    @Test
    void updateStockPreservesExistingQuantity() {
        UnitMaster unitMaster = unit("UNIT");
        ItemStock itemStock = new ItemStock();
        itemStock.setSku("SKU-1");
        itemStock.setRestaurantId(10L);
        itemStock.setMenuItem(directMenuItem());
        itemStock.setTotalStock(9);
        itemStock.setReorderLevel(2);
        itemStock.setUnitOfMeasure("PCS");
        itemStock.setIsActive(true);
        itemStock.setIsDeleted(false);

        when(inventoryUtil.getAccessibleStock("SKU-1")).thenReturn(itemStock);
        when(itemUnitConversionService.resolveBaseUnit(null, "UNIT", "PCS")).thenReturn(unitMaster);
        when(stockRepository.save(any(ItemStock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockResponse response = inventoryService.updateStock("SKU-1", new StockUpdateRequest(
                4,
                null,
                "UNIT",
                null,
                null,
                false
        ));

        assertThat(response.getTotalStock()).isEqualTo(9);
        assertThat(response.getReorderLevel()).isEqualTo(4);
        assertThat(response.getUnitOfMeasure()).isEqualTo("UNIT");
        assertThat(response.getIsActive()).isFalse();
    }

    private MenuItem directMenuItem() {
        Category category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Main");
        category.setCategoryDescription("Main");
        category.setIsActive(true);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(10L);
        menuItem.setRestaurantId(10L);
        menuItem.setItemName("Burger");
        menuItem.setDescription("Burger");
        menuItem.setMenuType(MenuType.DIRECT);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);
        menuItem.setIsAvailable(true);
        menuItem.setCategory(category);
        return menuItem;
    }

    private UnitMaster unit(String code) {
        UnitMaster unitMaster = new UnitMaster();
        unitMaster.setId(1L);
        unitMaster.setCode(code);
        unitMaster.setDisplayName(code);
        unitMaster.setActive(true);
        return unitMaster;
    }
}
