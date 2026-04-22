package com.kritik.POS.order.service.internal;

import com.kritik.POS.inventory.api.StockRequest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record OrderStockDemand(
        List<StockRequest> stockRequests,
        Map<String, Double> ingredientRequirements,
        Map<Long, Double> preparedRequirements,
        Set<Long> affectedMenuIds
) {
    public static OrderStockDemand empty() {
        return new OrderStockDemand(new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>());
    }
}
