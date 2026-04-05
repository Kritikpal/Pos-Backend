package com.kritik.POS.inventory.route;

public final class InventoryRoute {
    public static final String BASE = "/api/inventory";

    public static final String MENU_INGREDIENT_MAPPING = "/menuIngredientMapping";

    public static final String GET_STOCKS_PAGE = "/stocks";
    public static final String GET_STOCK = "/stocks/{sku}";
    public static final String SAVE_STOCK = "/stocks";
    public static final String UPDATE_STOCK = "/stocks/{sku}";

    public static final String GET_INGREDIENTS_PAGE = "/ingredients";
    public static final String GET_INGREDIENT = "/ingredients/{sku}";
    public static final String SAVE_INGREDIENT = "/ingredients";
    public static final String DELETE_INGREDIENT = "/ingredients/{sku}";

    public static final String GET_SUPPLIERS = "/suppliers";
    public static final String GET_SUPPLIERS_PAGE = "/suppliers/page";
    public static final String GET_SUPPLIER = "/suppliers/{id}";
    public static final String SAVE_SUPPLIER = "/suppliers";
    public static final String DELETE_SUPPLIER = "/suppliers/{id}";

    public static final String GET_RECEIPTS_PAGE = "/receipts";
    public static final String GET_RECEIPT_SKUS = "/receipts/skus";
    public static final String GET_RECEIPT = "/receipts/{id}";
    public static final String CREATE_RECEIPT = "/receipts";

    private InventoryRoute() {
    }
}
