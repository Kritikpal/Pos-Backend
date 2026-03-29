package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import com.kritik.POS.restaurant.models.request.StockReceiptCreateRequest;
import com.kritik.POS.restaurant.models.response.StockReceiptResponse;
import jakarta.transaction.Transactional;

public interface ReceiptService {
    PageResponse<StockReceiptResponseDto> getReceiptPage(Long chainId, Long restaurantId, String search, Integer pageNumber, Integer pageSize);

    StockReceiptResponse getReceiptById(Long receiptId);

    @Transactional
    StockReceiptResponse createStockReceipt(StockReceiptCreateRequest stockReceiptCreateRequest);
}
