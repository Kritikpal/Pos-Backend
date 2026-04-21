package com.kritik.POS.mobile.repository;

import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.mobile.repository.row.CategorySyncRow;
import com.kritik.POS.mobile.repository.row.IngredientStockSyncRow;
import com.kritik.POS.mobile.repository.row.ItemStockSyncRow;
import com.kritik.POS.mobile.repository.row.MenuItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuPriceSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeSyncRow;
import com.kritik.POS.mobile.repository.row.PosSettingSyncRow;
import com.kritik.POS.mobile.repository.row.PreparedStockSyncRow;
import com.kritik.POS.mobile.repository.row.TaxConfigSyncRow;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.ProductFile;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.tax.entity.TaxRate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PosSyncRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PosSyncRepository posSyncRepository;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        fixture = seedFixture();
    }

    @Test
    void findCategoryRowsFiltersByRestaurantAndOrdersByTimestampThenId() {
        List<CategorySyncRow> rows = posSyncRepository.findCategoryRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(CategorySyncRow::categoryId)
                .containsExactly(fixture.categoryOneId(), fixture.categoryTwoId(), fixture.deletedCategoryId());
        assertThat(rows).extracting(CategorySyncRow::isDeleted)
                .containsExactly(false, false, true);
    }

    @Test
    void findMenuItemRowsReturnsLiveAndDeletedRowsForRequestedRestaurant() {
        List<MenuItemSyncRow> rows = posSyncRepository.findMenuItemRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(MenuItemSyncRow::menuItemId)
                .containsExactly(fixture.menuItemOneId(), fixture.menuItemTwoId(), fixture.deletedMenuItemId());
        assertThat(rows.get(0).productImageUrl()).isEqualTo("uploads/menu-one.png");
    }

    @Test
    void findPriceRowsOnlyReturnsLiveMenuPrices() {
        List<MenuPriceSyncRow> rows = posSyncRepository.findPriceRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(MenuPriceSyncRow::menuItemId)
                .containsExactly(fixture.menuItemOneId(), fixture.menuItemTwoId());
        assertThat(rows).extracting(MenuPriceSyncRow::priceId)
                .doesNotContain(fixture.deletedPriceId());
    }

    @Test
    void findTaxRowsIncludesDeletedTaxRows() {
        List<TaxConfigSyncRow> rows = posSyncRepository.findTaxRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(TaxConfigSyncRow::taxId)
                .containsExactly(fixture.taxOneId(), fixture.deletedTaxId());
        assertThat(rows).extracting(TaxConfigSyncRow::isDeleted)
                .containsExactly(false, true);
    }

    @Test
    void findItemStockRowsAdvanceBySkuWhenTimestampMatches() {
        List<ItemStockSyncRow> rows = posSyncRepository.findItemStockRows(
                fixture.restaurantId(),
                fixture.syncTime(),
                fixture.itemStockOneSku(),
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(ItemStockSyncRow::sku)
                .containsExactly(fixture.itemStockTwoSku(), fixture.deletedItemStockSku());
    }

    @Test
    void findIngredientStockRowsReturnSupplierDataAndDeletedRows() {
        List<IngredientStockSyncRow> rows = posSyncRepository.findIngredientStockRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                "",
                PageRequest.of(0, 10)
        );

        assertThat(rows).extracting(IngredientStockSyncRow::sku)
                .containsExactly(fixture.ingredientOneSku(), fixture.deletedIngredientSku());
        assertThat(rows.get(0).supplierName()).isEqualTo("Supplier One");
    }

    @Test
    void findRecipeRowsUseParentMenuTimestamp() {
        List<MenuRecipeSyncRow> rows = posSyncRepository.findRecipeRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.menuItemId()).isEqualTo(fixture.menuItemOneId());
                    assertThat(row.syncUpdatedAt()).isEqualTo(fixture.syncTime());
                });
    }

    @Test
    void findRecipeItemRowsReturnLinkedIngredientData() {
        List<MenuRecipeItemSyncRow> rows = posSyncRepository.findRecipeItemRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.recipeId()).isEqualTo(fixture.recipeId());
                    assertThat(row.ingredientSku()).isEqualTo(fixture.ingredientOneSku());
                });
    }

    @Test
    void findPreparedStockRowsReturnPreparedQuantities() {
        List<PreparedStockSyncRow> rows = posSyncRepository.findPreparedStockRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.menuItemId()).isEqualTo(fixture.menuItemOneId());
                    assertThat(row.availableQty()).isEqualTo(12.0);
                });
    }

    @Test
    void findSettingRowsReturnRestaurantScopedSettings() {
        List<PosSettingSyncRow> rows = posSyncRepository.findSettingRows(
                fixture.restaurantId(),
                fixture.syncTime().minusMinutes(1),
                0L,
                PageRequest.of(0, 10)
        );

        assertThat(rows).singleElement()
                .satisfies(row -> {
                    assertThat(row.restaurantId()).isEqualTo(fixture.restaurantId());
                    assertThat(row.restaurantCode()).isEqualTo("REST-1");
                    assertThat(row.currency()).isEqualTo("INR");
                });
    }

    private Fixture seedFixture() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 1, 1, 10, 0);

        RestaurantChain chain = new RestaurantChain();
        chain.setName("Chain One");
        entityManager.persist(chain);

        Restaurant restaurant = buildRestaurant(chain, "REST-1", "Restaurant One");
        entityManager.persist(restaurant);

        Restaurant otherRestaurant = buildRestaurant(chain, "REST-2", "Restaurant Two");
        entityManager.persist(otherRestaurant);

        Supplier supplier = new Supplier();
        supplier.setRestaurantId(restaurant.getRestaurantId());
        supplier.setSupplierName("Supplier One");
        entityManager.persist(supplier);

        Category categoryOne = buildCategory(restaurant.getRestaurantId(), "Category One", false);
        Category categoryTwo = buildCategory(restaurant.getRestaurantId(), "Category Two", false);
        Category deletedCategory = buildCategory(restaurant.getRestaurantId(), "Category Deleted", true);
        Category otherCategory = buildCategory(otherRestaurant.getRestaurantId(), "Other Category", false);
        entityManager.persist(categoryOne);
        entityManager.persist(categoryTwo);
        entityManager.persist(deletedCategory);
        entityManager.persist(otherCategory);

        ProductFile productFile = new ProductFile();
        productFile.setFileName("menu-one.png");
        productFile.setFileType("image/png");
        productFile.setUrl("uploads/menu-one.png");
        entityManager.persist(productFile);

        ItemPrice priceOne = buildPrice(120.0, 10.0);
        ItemPrice priceTwo = buildPrice(150.0, 0.0);
        ItemPrice deletedPrice = buildPrice(180.0, 5.0);
        ItemPrice otherPrice = buildPrice(90.0, 0.0);
        entityManager.persist(priceOne);
        entityManager.persist(priceTwo);
        entityManager.persist(deletedPrice);
        entityManager.persist(otherPrice);

        MenuItem menuItemOne = buildMenuItem(restaurant.getRestaurantId(), categoryOne, priceOne, "Menu One", false);
        menuItemOne.setProductImage(productFile);
        menuItemOne.setHasRecipe(true);
        MenuItem menuItemTwo = buildMenuItem(restaurant.getRestaurantId(), categoryTwo, priceTwo, "Menu Two", false);
        MenuItem deletedMenuItem = buildMenuItem(restaurant.getRestaurantId(), categoryOne, deletedPrice, "Deleted Menu", true);
        MenuItem otherMenuItem = buildMenuItem(otherRestaurant.getRestaurantId(), otherCategory, otherPrice, "Other Menu", false);
        entityManager.persist(menuItemOne);
        entityManager.persist(menuItemTwo);
        entityManager.persist(deletedMenuItem);
        entityManager.persist(otherMenuItem);

        ItemStock itemStockOne = buildItemStock("STOCK-A", restaurant.getRestaurantId(), menuItemOne, supplier, false);
        ItemStock itemStockTwo = buildItemStock("STOCK-B", restaurant.getRestaurantId(), menuItemTwo, supplier, false);
        ItemStock deletedItemStock = buildItemStock("STOCK-Z", restaurant.getRestaurantId(), deletedMenuItem, supplier, true);
        ItemStock otherItemStock = buildItemStock("STOCK-OTHER", otherRestaurant.getRestaurantId(), otherMenuItem, null, false);
        entityManager.persist(itemStockOne);
        entityManager.persist(itemStockTwo);
        entityManager.persist(deletedItemStock);
        entityManager.persist(otherItemStock);

        IngredientStock ingredientOne = buildIngredient("ING-A", restaurant.getRestaurantId(), supplier, false);
        IngredientStock deletedIngredient = buildIngredient("ING-Z", restaurant.getRestaurantId(), supplier, true);
        IngredientStock otherIngredient = buildIngredient("ING-OTHER", otherRestaurant.getRestaurantId(), null, false);
        entityManager.persist(ingredientOne);
        entityManager.persist(deletedIngredient);
        entityManager.persist(otherIngredient);

        TaxRate taxOne = buildTax(restaurant.getRestaurantId(), "GST 5", false);
        TaxRate deletedTax = buildTax(restaurant.getRestaurantId(), "GST 18", true);
        TaxRate otherTax = buildTax(otherRestaurant.getRestaurantId(), "Other Tax", false);
        entityManager.persist(taxOne);
        entityManager.persist(deletedTax);
        entityManager.persist(otherTax);

        MenuRecipe recipe = new MenuRecipe();
        recipe.setMenuItem(menuItemOne);
        recipe.setBatchSize(4);
        recipe.setActive(true);
        entityManager.persist(recipe);

        MenuItemIngredient menuItemIngredient = new MenuItemIngredient();
        menuItemIngredient.setMenuItem(menuItemOne);
        menuItemIngredient.setRecipe(recipe);
        menuItemIngredient.setIngredientStock(ingredientOne);
        menuItemIngredient.setQuantityRequired(2.0);
        entityManager.persist(menuItemIngredient);

        PreparedItemStock preparedItemStock = new PreparedItemStock();
        preparedItemStock.setMenuItemId(menuItemOne.getId());
        preparedItemStock.setRestaurantId(restaurant.getRestaurantId());
        preparedItemStock.setAvailableQty(12.0);
        preparedItemStock.setReservedQty(1.0);
        preparedItemStock.setUnitCode("serving");
        preparedItemStock.setActive(true);
        entityManager.persist(preparedItemStock);

        entityManager.flush();

        setTimestamp("restaurant", "restaurant_id", restaurant.getRestaurantId(), syncTime);
        setTimestamp("category", "category_id", categoryOne.getCategoryId(), syncTime);
        setTimestamp("category", "category_id", categoryTwo.getCategoryId(), syncTime);
        setTimestamp("category", "category_id", deletedCategory.getCategoryId(), syncTime.plusMinutes(1));
        setTimestamp("menu_item", "id", menuItemOne.getId(), syncTime);
        setTimestamp("menu_item", "id", menuItemTwo.getId(), syncTime);
        setTimestamp("menu_item", "id", deletedMenuItem.getId(), syncTime.plusMinutes(1));
        setTimestamp("item_stock", "sku", itemStockOne.getSku(), syncTime);
        setTimestamp("item_stock", "sku", itemStockTwo.getSku(), syncTime);
        setTimestamp("item_stock", "sku", deletedItemStock.getSku(), syncTime.plusMinutes(1));
        setTimestamp("ingredient_stock", "sku", ingredientOne.getSku(), syncTime);
        setTimestamp("ingredient_stock", "sku", deletedIngredient.getSku(), syncTime.plusMinutes(1));
        setTimestamp("tax_rate", "tax_id", taxOne.getTaxId(), syncTime);
        setTimestamp("tax_rate", "tax_id", deletedTax.getTaxId(), syncTime.plusMinutes(1));
        setTimestamp("menu_item_ingredient", "id", menuItemIngredient.getId(), syncTime);
        setTimestamp("prepared_item_stock", "menu_item_id", preparedItemStock.getMenuItemId(), syncTime);
        entityManager.clear();

        return new Fixture(
                syncTime,
                restaurant.getRestaurantId(),
                categoryOne.getCategoryId(),
                categoryTwo.getCategoryId(),
                deletedCategory.getCategoryId(),
                menuItemOne.getId(),
                menuItemTwo.getId(),
                deletedMenuItem.getId(),
                deletedPrice.getPriceId(),
                taxOne.getTaxId(),
                deletedTax.getTaxId(),
                itemStockOne.getSku(),
                itemStockTwo.getSku(),
                deletedItemStock.getSku(),
                ingredientOne.getSku(),
                deletedIngredient.getSku(),
                recipe.getId()
        );
    }

    private Restaurant buildRestaurant(RestaurantChain chain, String code, String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setChain(chain);
        restaurant.setCode(code);
        restaurant.setName(name);
        restaurant.setCurrency("INR");
        restaurant.setTimezone("Asia/Kolkata");
        restaurant.setPhoneNumber("9999999999");
        restaurant.setEmail(name.toLowerCase().replace(" ", "") + "@example.com");
        restaurant.setGstNumber("GST-" + code);
        restaurant.setActive(true);
        restaurant.setDeleted(false);
        return restaurant;
    }

    private Category buildCategory(Long restaurantId, String name, boolean deleted) {
        Category category = new Category();
        category.setRestaurantId(restaurantId);
        category.setCategoryName(name);
        category.setCategoryDescription(name + " description");
        category.setIsActive(!deleted);
        category.setIsDeleted(deleted);
        return category;
    }

    private ItemPrice buildPrice(double price, double discount) {
        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(price);
        itemPrice.setDisCount(discount);
        return itemPrice;
    }

    private MenuItem buildMenuItem(Long restaurantId, Category category, ItemPrice price, String name, boolean deleted) {
        MenuItem menuItem = new MenuItem();
        menuItem.setRestaurantId(restaurantId);
        menuItem.setCategory(category);
        menuItem.setItemPrice(price);
        menuItem.setItemName(name);
        menuItem.setDescription(name + " description");
        menuItem.setIsAvailable(!deleted);
        menuItem.setIsActive(!deleted);
        menuItem.setIsDeleted(deleted);
        menuItem.setIsTrending(false);
        menuItem.setIsPrepared(false);
        menuItem.setHasRecipe(false);
        return menuItem;
    }

    private ItemStock buildItemStock(String sku,
                                     Long restaurantId,
                                     MenuItem menuItem,
                                     Supplier supplier,
                                     boolean deleted) {
        ItemStock itemStock = new ItemStock();
        itemStock.setSku(sku);
        itemStock.setRestaurantId(restaurantId);
        itemStock.setMenuItem(menuItem);
        itemStock.setSupplier(supplier);
        itemStock.setTotalStock(20);
        itemStock.setReorderLevel(5);
        itemStock.setUnitOfMeasure("pcs");
        itemStock.setIsActive(!deleted);
        itemStock.setIsDeleted(deleted);
        return itemStock;
    }

    private IngredientStock buildIngredient(String sku,
                                            Long restaurantId,
                                            Supplier supplier,
                                            boolean deleted) {
        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku(sku);
        ingredientStock.setRestaurantId(restaurantId);
        ingredientStock.setIngredientName(sku + "-name");
        ingredientStock.setDescription("Description " + sku);
        ingredientStock.setSupplier(supplier);
        ingredientStock.setTotalStock(15.0);
        ingredientStock.setReorderLevel(3.0);
        ingredientStock.setUnitOfMeasure("kg");
        ingredientStock.setIsActive(!deleted);
        ingredientStock.setIsDeleted(deleted);
        return ingredientStock;
    }

    private TaxRate buildTax(Long restaurantId, String name, boolean deleted) {
        TaxRate taxRate = new TaxRate();
        taxRate.setRestaurantId(restaurantId);
        taxRate.setTaxName(name);
        taxRate.setTaxAmount(deleted ? 18.0 : 5.0);
        taxRate.setActive(!deleted);
        taxRate.setDeleted(deleted);
        return taxRate;
    }

    private void setTimestamp(String table, String idColumn, Object idValue, LocalDateTime timestamp) {
        String valueClause = idValue instanceof String ? "'" + idValue + "'" : idValue.toString();
        entityManager.getEntityManager()
                .createNativeQuery(
                        "update " + table + " set created_at = :ts, updated_at = :ts where " + idColumn + " = " + valueClause
                )
                .setParameter("ts", timestamp)
                .executeUpdate();
    }

    private record Fixture(LocalDateTime syncTime,
                           Long restaurantId,
                           Long categoryOneId,
                           Long categoryTwoId,
                           Long deletedCategoryId,
                           Long menuItemOneId,
                           Long menuItemTwoId,
                           Long deletedMenuItemId,
                           Long deletedPriceId,
                           Long taxOneId,
                           Long deletedTaxId,
                           String itemStockOneSku,
                           String itemStockTwoSku,
                           String deletedItemStockSku,
                           String ingredientOneSku,
                           String deletedIngredientSku,
                           Long recipeId) {
    }
}
