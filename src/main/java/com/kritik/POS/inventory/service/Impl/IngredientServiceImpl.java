package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.master.Ingredient;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import com.kritik.POS.inventory.models.request.IngredientRequest;
import com.kritik.POS.inventory.models.response.IngredientResponse;
import com.kritik.POS.inventory.models.response.ItemUnitConversionResponse;
import com.kritik.POS.inventory.projection.IngredientStockListProjection;
import com.kritik.POS.inventory.repository.IngredientRepository;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.service.ItemUnitConversionService;
import com.kritik.POS.inventory.service.IngredientService;
import com.kritik.POS.inventory.util.InventoryUtil;
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
    private final IngredientRepository ingredientRepository;
    private final ItemUnitConversionService itemUnitConversionService;

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
        IngredientStock ingredientStock = inventoryUtil.getAccessibleIngredient(sku);
        IngredientResponse response = IngredientResponse.fromEntity(ingredientStock);
        response.setConversions(itemUnitConversionService.getConversions(
                        ingredientStock.getRestaurantId(),
                        UnitConversionSourceType.INGREDIENT,
                        ingredientStock.getSku())
                .stream()
                .map(ItemUnitConversionResponse::fromEntity)
                .toList());
        return response;
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
        Ingredient ingredient = ingredientRepository.findById(ingredientStock.getSku()).orElseGet(Ingredient::new);
        ingredient.setSku(ingredientStock.getSku());
        ingredient.setRestaurantId(restaurantId);
        ingredient.setIngredientName(ingredientRequest.ingredientName().trim());
        ingredient.setDescription(InventoryUtil.trimToNull(ingredientRequest.description()));
        ingredient.setCategory(InventoryUtil.trimToNull(ingredientRequest.category()));
        ingredient.setSupplier(ingredientRequest.supplierId() == null
                ? null
                : inventoryUtil.getAccessibleSupplier(ingredientRequest.supplierId(), restaurantId));
        UnitMaster baseUnit = itemUnitConversionService.resolveBaseUnit(
                ingredientRequest.baseUnitId(),
                ingredientRequest.unitOfMeasure(),
                ingredient.getBaseUnit() == null ? ingredientStock.getUnitOfMeasure() : ingredient.getBaseUnit().getCode()
        );
        ingredient.setBaseUnit(baseUnit);
        ingredient.setIsDeleted(false);
        if (ingredientRequest.isActive() != null) {
            ingredient.setIsActive(ingredientRequest.isActive());
        }
        ingredient = ingredientRepository.save(ingredient);

        ingredientStock.setIngredient(ingredient);
        ingredientStock.setRestaurantId(ingredient.getRestaurantId());
        ingredientStock.setIngredientName(ingredient.getIngredientName());
        ingredientStock.setDescription(ingredient.getDescription());
        ingredientStock.setCategory(ingredient.getCategory());
        ingredientStock.setSupplier(ingredient.getSupplier());
        ingredientStock.setTotalStock(ingredientStock.getTotalStock() == null ? 0.0 : ingredientStock.getTotalStock());
        ingredientStock.setReorderLevel(ingredientRequest.reorderLevel() == null ? 0.0 : ingredientRequest.reorderLevel());
        ingredientStock.setUnitOfMeasure(baseUnit.getCode());
        ingredientStock.setIsDeleted(false);
        if (ingredientRequest.isActive() != null) {
            ingredientStock.setIsActive(ingredientRequest.isActive());
        }

        IngredientStock savedIngredient = ingredientStockRepository.save(ingredientStock);
        itemUnitConversionService.updateConversionsForExistingItem(
                restaurantId,
                UnitConversionSourceType.INGREDIENT,
                savedIngredient.getSku(),
                baseUnit,
                ingredientRequest.conversions()
        );
        inventoryUtil.syncMenuAvailabilityForIngredient(savedIngredient.getSku());
        IngredientResponse response = IngredientResponse.fromEntity(savedIngredient);
        response.setConversions(itemUnitConversionService.getConversions(
                        restaurantId,
                        UnitConversionSourceType.INGREDIENT,
                        savedIngredient.getSku())
                .stream()
                .map(ItemUnitConversionResponse::fromEntity)
                .toList());
        return response;
    }

    @Transactional
    @Override
    public boolean deleteIngredient(String sku) {
        IngredientStock ingredientStock = inventoryUtil.getAccessibleIngredient(sku);
        Ingredient ingredient = ingredientRepository.findById(sku).orElse(null);
        if (ingredient != null) {
            ingredient.setIsDeleted(true);
            ingredient.setIsActive(false);
            ingredientRepository.save(ingredient);
        }
        ingredientStock.setIsDeleted(true);
        ingredientStock.setIsActive(false);
        ingredientStockRepository.save(ingredientStock);
        inventoryUtil.syncMenuAvailabilityForIngredient(sku);
        return true;
    }


}
