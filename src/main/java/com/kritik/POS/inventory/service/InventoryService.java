package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.api.StockRequest;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.request.StockUpdateRequest;
import com.kritik.POS.inventory.models.response.*;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.restaurant.entity.MenuItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryService {

    void checkStockAvailable(List<StockRequest> stockRequestList);

    List<StockReport> getAllStocks(String search, Integer limit);

    PageResponse<StockResponseDto> getStockPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize);


    StockResponse getStockBySku(String sku);

    List<UnitSummaryResponse> getAllUnits();

    StockResponse saveStock(ItemStockUpsertRequest itemStockUpsertRequest);

    StockResponse updateStock(String sku, StockUpdateRequest stockUpdateRequest);

    void deductStockForOrder(Order order);

    void restoreStockForRefund(Order order);

    void deductStockForRequirements(List<StockRequest> stockRequestList,
                                    Map<String, Double> ingredientRequirements,
                                    Map<Long, Double> preparedRequirements,
                                    Collection<Long> affectedMenuIds);

    void restoreStockForRequirements(List<StockRequest> stockRequestList,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements,
                                     Collection<Long> affectedMenuIds);

    void checkOrderStockAvailability(List<StockRequest> stockRequestList,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements);

    void refreshMenuAvailability(Collection<Long> menuIds, Collection<String> ingredientSkus);

    MenuItem getAccessibleMenuItem(Long menuItemId);
}
