package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.inventory.models.request.SupplierRequest;
import com.kritik.POS.inventory.models.response.SupplierResponse;
import com.kritik.POS.inventory.models.response.SupplierResponseDto;
import com.kritik.POS.inventory.repository.SupplierRepository;
import com.kritik.POS.inventory.service.SupplierService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.mapper.RestaurantDtoMapper;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {


    private final TenantAccessService tenantAccessService;
    private final SupplierRepository supplierRepository;
    private final RestaurantDtoMapper restaurantDtoMapper;
    private final InventoryUtil inventoryUtil;

    @Override
    public List<SupplierResponse> getSuppliers(Long chainId, Long restaurantId, Boolean isActive) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return supplierRepository.findVisibleSuppliers(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        isActive
                ).stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    public PageResponse<SupplierResponseDto> getSupplierPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<SupplierResponseDto> page = supplierRepository.findSupplierSummaries(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        isActive,
                        InventoryUtil.normalizeSearch(search),
                        PageRequest.of(pageNumber, pageSize)
                )
                .map(SupplierResponseDto::toSupplierDto);
        return PageResponse.from(page);
    }

    @Override
    public SupplierResponse getSupplierById(Long supplierId) {
        return SupplierResponse.fromEntity(inventoryUtil.getAccessibleSupplier(supplierId, null));
    }

    @Transactional
    @Override
    public SupplierResponse saveSupplier(SupplierRequest supplierRequest) {
        Supplier supplier = supplierRequest.supplierId() == null
                ? new Supplier()
                : inventoryUtil.getAccessibleSupplier(supplierRequest.supplierId(), null);

        Long restaurantId = tenantAccessService.resolveAccessibleRestaurantId(
                supplierRequest.restaurantId() != null ? supplierRequest.restaurantId() : supplier.getRestaurantId()
        );
        supplier.setRestaurantId(restaurantId);
        supplier.setSupplierName(supplierRequest.supplierName().trim());
        supplier.setContactPerson(InventoryUtil.trimToNull(supplierRequest.contactPerson()));
        supplier.setPhoneNumber(InventoryUtil.trimToNull(supplierRequest.phoneNumber()));
        supplier.setEmail(InventoryUtil.trimToNull(supplierRequest.email()));
        supplier.setAddress(InventoryUtil.trimToNull(supplierRequest.address()));
        supplier.setTaxIdentifier(InventoryUtil.trimToNull(supplierRequest.taxIdentifier()));
        supplier.setNotes(InventoryUtil.trimToNull(supplierRequest.notes()));
        supplier.setIsDeleted(false);
        if (supplierRequest.isActive() != null) {
            supplier.setIsActive(supplierRequest.isActive());
        }
        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional
    @Override
    public boolean deleteSupplier(Long supplierId) {
        Supplier supplier = inventoryUtil.getAccessibleSupplier(supplierId, null);
        supplier.setIsDeleted(true);
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
        return true;
    }

}
