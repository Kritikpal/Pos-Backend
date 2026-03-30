package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.request.StockUpdateRequest;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.models.response.StockResponse;

import java.util.List;

public interface InventoryService {
    StockReport getStockReport(String sku);

    void checkStockAvailable(List<StockRequest> stockRequestList);

    List<StockReport> getAllStocks(String search, Integer limit);

    PageResponse<StockResponseDto> getStockPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize);

    StockResponse getStockBySku(String sku);

    StockResponse saveStock(ItemStockUpsertRequest itemStockUpsertRequest);

    StockResponse updateStock(String sku, StockUpdateRequest stockUpdateRequest);


    void applyStockChangesForCompletedOrder(String orderId);

    MenuItem getAccessibleMenuItem(Long menuItemId);
}
