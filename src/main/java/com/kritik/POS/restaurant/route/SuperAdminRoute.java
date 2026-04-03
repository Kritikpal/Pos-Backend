package com.kritik.POS.restaurant.route;

public class SuperAdminRoute {

    private static final String BASE = "/api/admin";

    // 🔥 Setup
    public static final String SETUP_RESTAURANT = BASE + "/setup";

    // 🏢 Chain
    public static final String CREATE_CHAIN = BASE + "/chain";
    public static final String GET_ALL_CHAINS = BASE + "/chains";
    public static final String GET_CHAIN = BASE + "/chains/{chainId}";
    public static final String UPDATE_CHAIN = BASE + "/chains/{chainId}";

    // 🍽️ Restaurant
    public static final String CREATE_RESTAURANT = BASE + "/restaurant";
    public static final String GET_ALL_RESTAURANTS = BASE + "/restaurants";
    public static final String GET_RESTAURANT = BASE + "/restaurants/{restaurantId}";
    public static final String UPDATE_RESTAURANT = BASE + "/restaurants/{restaurantId}";

    // 👤 Admin
    public static final String CREATE_CHAIN_ADMIN = BASE + "/chain/{chainId}/admin";
    public static final String CREATE_RESTAURANT_ADMIN = BASE + "/restaurant/{restaurantId}/admin";
}
