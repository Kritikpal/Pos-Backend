package com.kritik.POS.inventory.route;

public final class InventoryRoute {
    public static final String BASE = "/api/inventory";

    public static final String MENU_INGREDIENT_MAPPING = "/menuIngredientMapping";
    public static final String SEARCH_RECIPE_MENU_ITEMS = "/recipes/menu-items";
    public static final String GET_RECIPE = "/recipes/{id}";
    public static final String SAVE_RECIPE = "/recipes";
    public static final String UPDATE_RECIPE = "/recipes/{id}";
    public static final String DELETE_RECIPE = "/recipes/{id}";

    public static final String GET_STOCKS_PAGE = "/stocks";
    public static final String GET_STOCK = "/stocks/{sku}";
    public static final String SAVE_STOCK = "/stocks";
    public static final String UPDATE_STOCK = "/stocks/{sku}";

    public static final String GET_PREPARED_STOCKS_PAGE = "/prepared-stocks";
    public static final String GET_PREPARED_STOCK = "/prepared-stocks/{menuItemId}";
    public static final String UPDATE_PREPARED_STOCK = "/prepared-stocks/{menuItemId}";

    public static final String GET_INGREDIENTS_PAGE = "/ingredients";
    public static final String GET_INGREDIENTS_PAGE_V2 = "/v2/ingredients";
    public static final String GET_INGREDIENT = "/ingredients/{sku}";
    public static final String SAVE_INGREDIENT = "/ingredients";
    public static final String DELETE_INGREDIENT = "/ingredients/{sku}";
    public static final String PREVIEW_INGREDIENT_IMPORT = "/ingredients/import/preview";
    public static final String COMMIT_INGREDIENT_IMPORT = "/ingredients/import/commit";

    public static final String GET_SUPPLIERS = "/suppliers";
    public static final String GET_SUPPLIERS_PAGE = "/suppliers/page";
    public static final String GET_SUPPLIER = "/suppliers/{id}";
    public static final String SAVE_SUPPLIER = "/suppliers";
    public static final String DELETE_SUPPLIER = "/suppliers/{id}";

    public static final String GET_RECEIPTS_PAGE = "/receipts";
    public static final String GET_RECEIPT_SKUS = "/receipts/skus";
    public static final String GET_RECEIPT = "/receipts/{id}";
    public static final String CREATE_RECEIPT = "/receipts";

    public static final String GET_PRODUCTION_ENTRIES = "/production-entries";
    public static final String GET_PRODUCTION_ENTRY = "/production-entries/{id}";
    public static final String GET_COOKED_MENUS = "/cooked-menus";
    public static final String CREATE_PRODUCTION_ENTRY = "/production-entries";

    private InventoryRoute() {
    }
}
