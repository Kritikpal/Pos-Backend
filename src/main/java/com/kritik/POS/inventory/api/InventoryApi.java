package com.kritik.POS.inventory.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface InventoryApi {

    void checkOrderStockAvailability(List<StockRequest> stockRequests,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements);

    void deductStockForRequirements(List<StockRequest> stockRequests,
                                    Map<String, Double> ingredientRequirements,
                                    Map<Long, Double> preparedRequirements,
                                    Collection<Long> affectedMenuIds);

    void restoreStockForRequirements(List<StockRequest> stockRequests,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements,
                                     Collection<Long> affectedMenuIds);
}
