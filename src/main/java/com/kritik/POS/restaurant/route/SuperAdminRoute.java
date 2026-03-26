package com.kritik.POS.restaurant.route;

public class SuperAdminRoute {

    private static final String BASE = "/api/admin";

    // 🔥 Setup
    public static final String SETUP_RESTAURANT = BASE + "/setup";

    // 🏢 Chain
    public static final String CREATE_CHAIN = BASE + "/chain";
    public static final String GET_ALL_CHAINS = BASE + "/chains";

    // 🍽️ Restaurant
    public static final String CREATE_RESTAURANT = BASE + "/restaurant";
    public static final String GET_ALL_RESTAURANTS = BASE + "/restaurants";

    // 👤 Admin
    public static final String CREATE_CHAIN_ADMIN = BASE + "/chain/{chainId}/admin";
    public static final String CREATE_RESTAURANT_ADMIN = BASE + "/restaurant/{restaurantId}/admin";
}