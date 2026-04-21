package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.PreparedStockUpdateRequest;
import com.kritik.POS.inventory.models.response.PreparedStockResponseDto;

public interface PreparedStockService {

    PageResponse<PreparedStockResponseDto> getPreparedStockPage(Long chainId,
                                                                Long restaurantId,
                                                                String search,
                                                                Integer pageNumber,
                                                                Integer pageSize);

    PreparedStockResponseDto getPreparedStock(Long menuItemId);

    PreparedStockResponseDto updatePreparedStock(Long menuItemId, PreparedStockUpdateRequest request);
}
