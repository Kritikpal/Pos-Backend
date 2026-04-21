package com.kritik.POS.mobile.dto.request;

import lombok.Data;

@Data
public class SyncTimeCursorBundle {

    private TimeCursorDto categories;
    private TimeCursorDto menuItems;
    private TimeCursorDto prices;
    private TimeCursorDto taxes;
    private TimeCursorDto taxClasses;
    private TimeCursorDto taxDefinitions;
    private TimeCursorDto taxRules;
    private TimeCursorDto taxRegistrations;
    private TimeCursorDto itemStocks;
    private TimeCursorDto ingredientStocks;
    private TimeCursorDto recipes;
    private TimeCursorDto recipeItems;
    private TimeCursorDto preparedStocks;
    private TimeCursorDto settings;
}
