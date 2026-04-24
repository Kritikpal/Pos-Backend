package com.kritik.POS.tax.route;

public class TaxRoute {
    public static final String BASE = "/api/restaurants/tax";

    public static final String GET_TAX_CLASSES = BASE + "/classes";
    public static final String SAVE_TAX_CLASS = BASE + "/classes";
    public static final String DELETE_TAX_CLASS = BASE + "/classes/{id}";

    public static final String GET_TAX_DEFINITIONS = BASE + "/definitions";
    public static final String SAVE_TAX_DEFINITION = BASE + "/definitions";
    public static final String DELETE_TAX_DEFINITION = BASE + "/definitions/{id}";

    public static final String GET_TAX_RULES = BASE + "/rules";
    public static final String SAVE_TAX_RULE = BASE + "/rules";
    public static final String DELETE_TAX_RULE = BASE + "/rules/{id}";

    public static final String GET_TAX_REGISTRATIONS = BASE + "/registrations";
    public static final String SAVE_TAX_REGISTRATION = BASE + "/registrations";
    public static final String DELETE_TAX_REGISTRATION = BASE + "/registrations/{id}";
    public static final String SEED_TAX_CATALOG = BASE + "/catalog/seed";

    private TaxRoute() {
    }
}
