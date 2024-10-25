package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.restaurant.DAO.ItemStock;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.repository.StockRepository;
import com.kritik.POS.restaurant.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockServiceImpl implements StockService {
    private final StockRepository stockRepository;

    @Autowired
    public StockServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    @Override
    public StockReport getStockReport(String sku) {
        ItemStock itemStock = stockRepository.findById(sku).orElseThrow(() -> new AppException("Stock not found", HttpStatus.BAD_REQUEST));
        return StockReport.buildStockReport(itemStock);
    }

    @Override
    public void checkStockAvailable(List<StockRequest> stockRequestList) {
        for (StockRequest stockRequest : stockRequestList) {
            String sku = stockRequest.sku();
            ItemStock itemStock = stockRepository.findById(sku).orElseThrow(() -> new StockException("Stock not found"));
            if (itemStock.getTotalStock() - stockRequest.amount() < 0) {
                String itemName = itemStock.getMenuItem().getItemName();
                throw new StockException(itemName + " is not available in stock only " + itemStock.getTotalStock() + " left.");
            }
        }
    }

    @Override
    public List<StockReport> getALlStocks(String search, Integer limit) {
        if (limit == null || limit == 0) {
            limit = 5;
        }
        Page<ItemStock> allOrderByTotalStock = stockRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC,"totalStock")));
        return allOrderByTotalStock.stream().map(StockReport::buildStockReport).toList();
    }


}
