package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.projection.IngredientStockListProjection;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.service.IngredientService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.models.request.IngredientRequest;
import com.kritik.POS.restaurant.models.response.IngredientResponse;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {


    private final InventoryUtil inventoryUtil;
    private final TenantAccessService tenantAccessService;
    private final IngredientStockRepository ingredientStockRepository;

    @Override
    public PageResponse<IngredientResponse> getIngredientPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<IngredientResponse> page = ingredientStockRepository.findVisibleIngredients(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        isActive,
                        Boolean.TRUE.equals(lowStockOnly),
                        InventoryUtil.normalizeSearch(search),
                        PageRequest.of(pageNumber, pageSize)
                )
                .map(IngredientResponse::fromEntity);
        return PageResponse.from(page);
    }

    @Override
    public PageResponse<IngredientStockListProjection> getIngredientPageV2(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }

        Page<IngredientStockListProjection> page = ingredientStockRepository.findVisibleIngredientsV2(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                isActive,
                Boolean.TRUE.equals(lowStockOnly),
                InventoryUtil.normalizeSearch(search),
                PageRequest.of(pageNumber, pageSize)
        );
        return PageResponse.from(page);
    }


    @Override
    public IngredientResponse getIngredientBySku(String sku) {
        return IngredientResponse.fromEntity(inventoryUtil.getAccessibleIngredient(sku));
    }

    @Transactional
    @Override
    public IngredientResponse saveIngredient(IngredientRequest ingredientRequest) {
        IngredientStock ingredientStock = ingredientRequest.sku() == null || ingredientRequest.sku().isBlank()
                ? new IngredientStock()
                : inventoryUtil.getAccessibleIngredient(ingredientRequest.sku());

        Long restaurantId = tenantAccessService.resolveAccessibleRestaurantId(
                ingredientRequest.restaurantId() != null ? ingredientRequest.restaurantId() : ingredientStock.getRestaurantId()
        );

        if (ingredientStock.getSku() == null) {
            ingredientStock.setSku(UUID.randomUUID().toString());
        }
        ingredientStock.setRestaurantId(restaurantId);
        ingredientStock.setIngredientName(ingredientRequest.ingredientName().trim());
        ingredientStock.setDescription(InventoryUtil.trimToNull(ingredientRequest.description()));
        ingredientStock.setCategory(InventoryUtil.trimToNull(ingredientRequest.category()));
        ingredientStock.setSupplier(ingredientRequest.supplierId() == null
                ? null
                : inventoryUtil.getAccessibleSupplier(ingredientRequest.supplierId(), restaurantId));
        ingredientStock.setTotalStock(ingredientStock.getTotalStock() == null ? 0.0 : ingredientStock.getTotalStock());
        ingredientStock.setReorderLevel(ingredientRequest.reorderLevel() == null ? 0.0 : ingredientRequest.reorderLevel());
        ingredientStock.setUnitOfMeasure(
                ingredientRequest.unitOfMeasure() == null || ingredientRequest.unitOfMeasure().isBlank()
                        ? ingredientStock.getUnitOfMeasure() == null ? "unit" : ingredientStock.getUnitOfMeasure()
                        : ingredientRequest.unitOfMeasure().trim()
        );
        ingredientStock.setIsDeleted(false);
        if (ingredientRequest.isActive() != null) {
            ingredientStock.setIsActive(ingredientRequest.isActive());
        }

        IngredientStock savedIngredient = ingredientStockRepository.save(ingredientStock);
        inventoryUtil.syncMenuAvailabilityForIngredient(savedIngredient.getSku());
        return IngredientResponse.fromEntity(savedIngredient);
    }

    @Transactional
    @Override
    public boolean deleteIngredient(String sku) {
        IngredientStock ingredientStock = inventoryUtil.getAccessibleIngredient(sku);
        ingredientStock.setIsDeleted(true);
        ingredientStock.setIsActive(false);
        ingredientStockRepository.save(ingredientStock);
        inventoryUtil.syncMenuAvailabilityForIngredient(sku);
        return true;
    }


}
