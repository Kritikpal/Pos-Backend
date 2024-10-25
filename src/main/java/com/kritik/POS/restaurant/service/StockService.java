package com.kritik.POS.restaurant.service;

import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.response.StockReport;

import java.util.List;

public interface StockService {
    StockReport getStockReport(String sku);

    void checkStockAvailable(List<StockRequest> stockRequestList);

    List<StockReport> getALlStocks(String search,Integer limit);


}
