package com.kritik.POS.inventory.service;

import com.kritik.POS.inventory.api.InventoryApi;
import com.kritik.POS.inventory.api.StockRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryApiImpl implements InventoryApi {

    private final InventoryService inventoryService;

    @Override
    public void checkOrderStockAvailability(List<StockRequest> stockRequests,
                                            Map<String, Double> ingredientRequirements,
                                            Map<Long, Double> preparedRequirements) {
        inventoryService.checkOrderStockAvailability(stockRequests, ingredientRequirements, preparedRequirements);
    }

    @Override
    public void deductStockForRequirements(List<StockRequest> stockRequests,
                                           Map<String, Double> ingredientRequirements,
                                           Map<Long, Double> preparedRequirements,
                                           Collection<Long> affectedMenuIds) {
        inventoryService.deductStockForRequirements(stockRequests, ingredientRequirements, preparedRequirements, affectedMenuIds);
    }

    @Override
    public void restoreStockForRequirements(List<StockRequest> stockRequests,
                                            Map<String, Double> ingredientRequirements,
                                            Map<Long, Double> preparedRequirements,
                                            Collection<Long> affectedMenuIds) {
        inventoryService.restoreStockForRequirements(stockRequests, ingredientRequirements, preparedRequirements, affectedMenuIds);
    }
}
