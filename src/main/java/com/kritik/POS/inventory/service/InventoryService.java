package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.request.StockUpdateRequest;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.models.response.StockResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryService {

    void checkStockAvailable(List<StockRequest> stockRequestList);

    List<StockReport> getAllStocks(String search, Integer limit);

    PageResponse<StockResponseDto> getStockPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize);

    List<MenuItemIngredientDto> getIngredientMenuMapping(Long chainId, Long restaurantId);

    StockResponse getStockBySku(String sku);

    StockResponse saveStock(ItemStockUpsertRequest itemStockUpsertRequest);

    StockResponse updateStock(String sku, StockUpdateRequest stockUpdateRequest);

    void deductStockForOrder(Order order);

    void restoreStockForRefund(Order order);

    void checkOrderStockAvailability(List<StockRequest> stockRequestList,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements);

    void refreshMenuAvailability(Collection<Long> menuIds, Collection<String> ingredientSkus);

    MenuItem getAccessibleMenuItem(Long menuItemId);
}
