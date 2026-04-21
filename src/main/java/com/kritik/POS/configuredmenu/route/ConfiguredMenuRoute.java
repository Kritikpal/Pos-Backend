package com.kritik.POS.configuredmenu.route;

public final class ConfiguredMenuRoute {

    public static final String BASE = "/api/configured-menus";
    public static final String SEARCH_MENU_ITEMS = "/menu-items/search";
    public static final String GET_TEMPLATE = "/{id}";
    public static final String GET_TEMPLATE_PREVIEW = "/{id}/preview";
    public static final String DELETE_TEMPLATE = "/{id}";

    private ConfiguredMenuRoute() {
    }
}
