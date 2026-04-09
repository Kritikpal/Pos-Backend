package com.kritik.POS.restaurant.dto;

public record RestaurantDataDeletionResponseDto(
        Long restaurantId,
        String restaurantName,
        int deletedMenuCount,
        int deletedCategoryCount,
        int deletedTableCount,
        int deletedIngredientCount,
        int deletedSupplierCount,
        int deletedReceiptCount,
        int deletedTaxCount,
        int deletedOrderCount,
        int deletedInvoiceCount
) {
}
