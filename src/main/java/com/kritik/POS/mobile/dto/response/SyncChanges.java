package com.kritik.POS.mobile.dto.response;

import com.kritik.POS.mobile.dto.response.syncDtos.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncChanges {

    @Builder.Default
    private List<CategorySyncDto> categories = new ArrayList<>();

    @Builder.Default
    private List<MenuItemSyncDto> menuItems = new ArrayList<>();

    @Builder.Default
    private List<MenuPriceSyncDto> prices = new ArrayList<>();

    @Builder.Default
    private List<TaxConfigSyncDto> taxes = new ArrayList<>();

    @Builder.Default
    private List<TaxClassSyncDto> taxClasses = new ArrayList<>();

    @Builder.Default
    private List<TaxDefinitionSyncDto> taxDefinitions = new ArrayList<>();

    @Builder.Default
    private List<TaxRuleSyncDto> taxRules = new ArrayList<>();

    @Builder.Default
    private List<TaxRegistrationSyncDto> taxRegistrations = new ArrayList<>();

    @Builder.Default
    private List<ItemStockSyncDto> itemStocks = new ArrayList<>();

    @Builder.Default
    private List<IngredientStockSyncDto> ingredientStocks = new ArrayList<>();

    @Builder.Default
    private List<MenuRecipeSyncDto> recipes = new ArrayList<>();

    @Builder.Default
    private List<MenuRecipeItemSyncDto> recipeItems = new ArrayList<>();

    @Builder.Default
    private List<PreparedStockSyncDto> preparedStocks = new ArrayList<>();

    @Builder.Default
    private List<PosSettingSyncDto> settings = new ArrayList<>();
}
