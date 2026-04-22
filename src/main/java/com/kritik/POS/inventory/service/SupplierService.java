package com.kritik.POS.inventory.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.SupplierRequest;
import com.kritik.POS.inventory.models.response.SupplierResponse;
import com.kritik.POS.inventory.models.response.SupplierResponseDto;
import jakarta.transaction.Transactional;

import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getSuppliers(Long chainId, Long restaurantId, Boolean isActive);

    PageResponse<SupplierResponseDto> getSupplierPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize);

    SupplierResponse getSupplierById(Long supplierId);

    @Transactional
    SupplierResponse saveSupplier(SupplierRequest supplierRequest);

    @Transactional
    boolean deleteSupplier(Long supplierId);
}
