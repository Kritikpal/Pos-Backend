package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.ProductionEntryCreateRequest;
import com.kritik.POS.inventory.models.response.ProductionEntryResponseDto;
import com.kritik.POS.inventory.models.response.ProductionEntrySummaryDto;

public interface ProductionEntryService {

    ProductionEntryResponseDto createProductionEntry(ProductionEntryCreateRequest request);

    PageResponse<ProductionEntrySummaryDto> getProductionEntryPage(Long chainId,
                                                                   Long restaurantId,
                                                                   Long menuItemId,
                                                                   Integer pageNumber,
                                                                   Integer pageSize);

    ProductionEntryResponseDto getProductionEntry(Long id);
}
