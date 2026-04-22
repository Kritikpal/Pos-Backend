package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.StockReceiptCreateRequest;
import com.kritik.POS.inventory.models.response.StockReceiptResponse;
import com.kritik.POS.inventory.models.response.StockReceiptSkuOptionDto;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface ReceiptService {
    PageResponse<StockReceiptResponseDto> getReceiptPage(Long chainId, Long restaurantId, String search, Integer pageNumber, Integer pageSize);

    List<StockReceiptSkuOptionDto> getReceiptSkuOptions(Long supplierId);

    StockReceiptResponse getReceiptById(Long receiptId);

    StockReceiptResponse createStockReceipt(StockReceiptCreateRequest stockReceiptCreateRequest);
}
