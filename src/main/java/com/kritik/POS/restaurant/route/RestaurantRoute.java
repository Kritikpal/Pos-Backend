package com.kritik.POS.restaurant.route;

public class RestaurantRoute {
    public static final String GET_RESTAURANT_DASHBOARD = "/getDashboard";

    public static final String BULK_UPLOAD_MENU_ITEMS = "/addItemToRestaurant";
    public static final String BULK_UPLOAD_TABLES = "/addTableToRestaurant";

    public static final String GET_ALL_ITEMS = "/getAllItems";
    public static final String GET_MENU_ITEM = "/getMenuItem/{id}";
    public static final String EDIT_ADD_MENU_ITEM = "/menuEdit";
    public static final String DELETE_MENU_ITEM = "/deleteMenuItem/{id}";
    public static final String DELETE_ALL_MENU_ITEMS = "/deleteAllMenuItems";


    public static final String GET_ALL_CATEGORIES = "/getAllCategories";
    public static final String GET_CATEGORY = "/getCategory/{id}";
    public static final String EDIT_ADD_CATEGORY = "/saveCategory";
    public static final String DELETE_CATEGORY = "/deleteCategory/{id}";

    public static final String GET_ALL_TABLES = "/getAllTables";
    public static final String GET_TABLE = "/getTableById/{id}";
    public static final String EDIT_ADD_TABLE = "/tableEdit";
    public static final String DELETE_TABLE = "/deleteTable";

}
