package com.kritik.POS.mobile.service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

enum SyncGroup {
    CATEGORIES("categories"),
    MENU_ITEMS("menuItems"),
    PRICES("prices"),
    TAXES("taxes"),
    TAX_CLASSES("taxClasses"),
    TAX_DEFINITIONS("taxDefinitions"),
    TAX_RULES("taxRules"),
    TAX_REGISTRATIONS("taxRegistrations"),
    ITEM_STOCK("itemStocks"),
    INGREDIENT_STOCK("ingredientStocks"),
    RECIPES("recipes"),
    RECIPE_ITEMS("recipeItems"),
    PREPARED_STOCKS("preparedStocks"),
    SETTINGS("settings");

    private final String groupName;

    SyncGroup(String groupName) {
        this.groupName = groupName;
    }

    public String groupName() {
        return groupName;
    }

    public static Set<String> allGroupNames() {
        Set<String> groups = new LinkedHashSet<>();
        for (SyncGroup group : values()) {
            groups.add(group.groupName);
        }
        return groups;
    }

    public static SyncGroup fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(group -> group.groupName.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElse(null);
    }
}
