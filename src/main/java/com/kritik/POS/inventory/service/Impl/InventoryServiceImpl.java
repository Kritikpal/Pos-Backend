package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.request.StockUpdateRequest;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.models.response.StockResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.restaurant.specification.MenuItemSpecification;
import com.kritik.POS.restaurant.util.InventoryAvailabilityUtil;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final StockRepository stockRepository;
    private final IngredientStockRepository ingredientStockRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemIngredientRepository menuItemIngredientRepository;
    private final SaleItemRepository saleItemRepository;
    private final TenantAccessService tenantAccessService;
    private final InventoryUtil inventoryUtil;


    @Override
    public void checkStockAvailable(List<StockRequest> stockRequestList) {
        for (StockRequest stockRequest : stockRequestList) {
            ItemStock itemStock = inventoryUtil.getAccessibleStock(stockRequest.sku());
            if (!Boolean.TRUE.equals(itemStock.getIsActive())) {
                throw new StockException(itemStock.getMenuItem().getItemName() + " stock is inactive.");
            }
            if (itemStock.getTotalStock() - stockRequest.amount() < 0) {
                throw new StockException(itemStock.getMenuItem().getItemName() + " is not available in stock only " + itemStock.getTotalStock() + " left.");
            }
        }
    }

    @Override
    public List<StockReport> getAllStocks(String search, Integer limit) {
        int resolvedLimit = (limit == null || limit == 0) ? 5 : limit;
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        Page<ItemStock> allOrderByTotalStock = stockRepository.findVisibleStocks(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                PageRequest.of(0, resolvedLimit, Sort.by(Sort.Direction.ASC, "totalStock"))
        );
        return allOrderByTotalStock.stream().map(StockReport::buildStockReport).toList();
    }

    @Override
    public PageResponse<StockResponseDto> getStockPage(Long chainId, Long restaurantId, Boolean isActive, Boolean lowStockOnly, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<StockResponseDto> page = stockRepository.findStockSummaries(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        isActive,
                        Boolean.TRUE.equals(lowStockOnly),
                        InventoryUtil.normalizeSearch(search),
                        PageRequest.of(pageNumber, pageSize)
                )
                .map(StockResponseDto::toStockDto);
        return PageResponse.from(page);
    }

    @Override
    public List<MenuItemIngredientDto> getIngredientMenuMapping(Long chainId, Long restaurantId) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return menuItemIngredientRepository.findAllForRestaurant(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
                ).stream().map(MenuItemIngredientDto::fromProjection).toList();
    }

    @Override
    public StockResponse getStockBySku(String sku) {
        return StockResponse.fromEntity(inventoryUtil.getAccessibleStock(sku));
    }

    @Override
    @Transactional
    public StockResponse saveStock(ItemStockUpsertRequest itemStockUpsertRequest) {
        MenuItem menuItem = getAccessibleMenuItem(itemStockUpsertRequest.menuItemId());
        if (InventoryAvailabilityUtil.hasRecipe(menuItem)) {
            throw new AppException("Recipe-based menu items are managed through ingredient inventory", HttpStatus.BAD_REQUEST);
        }

        ItemStock itemStock = menuItem.getItemStock() == null ? new ItemStock() : menuItem.getItemStock();
        Integer previousTotalStock = itemStock.getTotalStock();

        if (itemStock.getSku() == null) {
            itemStock.setSku(UUID.randomUUID().toString());
            itemStock.setMenuItem(menuItem);
            menuItem.setItemStock(itemStock);
        }

        itemStock.setRestaurantId(menuItem.getRestaurantId());
        itemStock.setTotalStock(itemStockUpsertRequest.totalStock());
        itemStock.setReorderLevel(itemStockUpsertRequest.reorderLevel() == null ? 0 : itemStockUpsertRequest.reorderLevel());
        itemStock.setUnitOfMeasure(
                itemStockUpsertRequest.unitOfMeasure() == null || itemStockUpsertRequest.unitOfMeasure().isBlank()
                        ? "unit"
                        : itemStockUpsertRequest.unitOfMeasure().trim()
        );
        itemStock.setSupplier(itemStockUpsertRequest.supplierId() == null
                ? null
                : inventoryUtil.getAccessibleSupplier(itemStockUpsertRequest.supplierId(), menuItem.getRestaurantId()));
        itemStock.setIsDeleted(false);
        itemStock.setIsActive(itemStockUpsertRequest.isActive() != null ? itemStockUpsertRequest.isActive() : Boolean.TRUE);
        if (previousTotalStock == null || itemStock.getTotalStock() > previousTotalStock) {
            itemStock.setLastRestockedAt(LocalDateTime.now());
        }

        InventoryUtil.syncMenuAvailability(menuItem);
        return StockResponse.fromEntity(stockRepository.save(itemStock));
    }

    @Override
    @Transactional
    public StockResponse updateStock(String sku, StockUpdateRequest stockUpdateRequest) {
        ItemStock itemStock = inventoryUtil.getAccessibleStock(sku);
        Integer previousTotalStock = itemStock.getTotalStock();
        if (stockUpdateRequest.totalStock() != null) {
            itemStock.setTotalStock(stockUpdateRequest.totalStock());
            if (previousTotalStock == null || stockUpdateRequest.totalStock() > previousTotalStock) {
                itemStock.setLastRestockedAt(LocalDateTime.now());
            }
        }
        if (stockUpdateRequest.reorderLevel() != null) {
            itemStock.setReorderLevel(stockUpdateRequest.reorderLevel());
        }
        if (stockUpdateRequest.unitOfMeasure() != null && !stockUpdateRequest.unitOfMeasure().isBlank()) {
            itemStock.setUnitOfMeasure(stockUpdateRequest.unitOfMeasure().trim());
        }
        if (stockUpdateRequest.isActive() != null) {
            itemStock.setIsActive(stockUpdateRequest.isActive());
        }
        if (stockUpdateRequest.supplierId() != null) {
            itemStock.setSupplier(inventoryUtil.getAccessibleSupplier(stockUpdateRequest.supplierId(), itemStock.getRestaurantId()));
        }
        InventoryUtil.syncMenuAvailability(itemStock);
        return StockResponse.fromEntity(stockRepository.save(itemStock));
    }


    @Override
    @Transactional
    public void deductStockForOrder(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new AppException("Order not found", HttpStatus.BAD_REQUEST);
        }

        String orderId = order.getOrderId();
        LocalDateTime updatedAt = LocalDateTime.now();
        List<DirectStockDeductionProjection> directStockDeductions = saleItemRepository.findDirectStockDeductionsByOrderId(orderId);
        List<IngredientStockDeductionProjection> ingredientStockDeductions = saleItemRepository.findIngredientStockDeductionsByOrderId(orderId);

        for (DirectStockDeductionProjection deduction : directStockDeductions) {
            int updatedRows = stockRepository.deductStockQuantityIfAvailable(
                    deduction.getSku(),
                    deduction.getQuantity().intValue(),
                    updatedAt
            );
            if (updatedRows == 0) {
                throw new StockException("Insufficient stock for sku " + deduction.getSku());
            }
        }

        for (IngredientStockDeductionProjection deduction : ingredientStockDeductions) {
            int updatedRows = ingredientStockRepository.deductStockQuantityIfAvailable(
                    deduction.getSku(),
                    deduction.getQuantity(),
                    updatedAt
            );
            if (updatedRows == 0) {
                throw new StockException("Insufficient ingredient stock for sku " + deduction.getSku());
            }
        }

        refreshAffectedMenuAvailability(
                saleItemRepository.findDistinctDirectMenuIdsByOrderId(orderId),
                ingredientStockDeductions.stream()
                        .map(IngredientStockDeductionProjection::getSku)
                        .collect(Collectors.toSet())
        );
    }

    @Override
    @Transactional
    public void restoreStockForRefund(Order order) {
        if (order == null || order.getOrderId() == null) {
            throw new AppException("Order not found", HttpStatus.BAD_REQUEST);
        }

        String orderId = order.getOrderId();
        LocalDateTime updatedAt = LocalDateTime.now();
        List<DirectStockDeductionProjection> directStockDeductions = saleItemRepository.findDirectStockDeductionsByOrderId(orderId);
        List<IngredientStockDeductionProjection> ingredientStockDeductions = saleItemRepository.findIngredientStockDeductionsByOrderId(orderId);

        for (DirectStockDeductionProjection deduction : directStockDeductions) {
            int updatedRows = stockRepository.increaseStockQuantity(
                    deduction.getSku(),
                    deduction.getQuantity().intValue(),
                    updatedAt
            );
            if (updatedRows == 0) {
                throw new StockException("Stock not found for sku " + deduction.getSku());
            }
        }

        for (IngredientStockDeductionProjection deduction : ingredientStockDeductions) {
            int updatedRows = ingredientStockRepository.increaseStockQuantity(
                    deduction.getSku(),
                    deduction.getQuantity(),
                    updatedAt
            );
            if (updatedRows == 0) {
                throw new StockException("Ingredient stock not found for sku " + deduction.getSku());
            }
        }

        refreshAffectedMenuAvailability(
                saleItemRepository.findDistinctDirectMenuIdsByOrderId(orderId),
                ingredientStockDeductions.stream()
                        .map(IngredientStockDeductionProjection::getSku)
                        .collect(Collectors.toSet())
        );
    }

    private void refreshAffectedMenuAvailability(Collection<Long> directMenuIds, Collection<String> ingredientSkus) {
        if (directMenuIds != null && !directMenuIds.isEmpty()) {
            menuItemRepository.markUnavailableByIds(directMenuIds);
            menuItemRepository.markDirectMenusAvailableByIds(directMenuIds);
        }

        if (ingredientSkus == null || ingredientSkus.isEmpty()) {
            return;
        }

        List<Long> recipeMenuIds = menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(ingredientSkus);
        if (recipeMenuIds.isEmpty()) {
            return;
        }
        menuItemRepository.refreshRecipeAvailabilityByIds(recipeMenuIds);
    }

    @Override
    public MenuItem getAccessibleMenuItem(Long menuItemId) {
        Specification<MenuItem> specification = Specification.where(MenuItemSpecification.hasId(menuItemId))
                .and(MenuItemSpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            specification = specification.and(
                    MenuItemSpecification.belongsToRestaurants(tenantAccessService.resolveAccessibleRestaurantIds(null, null))
            );
        }
        return menuItemRepository.findOne(specification)
                .orElseThrow(() -> new AppException("Menu Item is not valid", HttpStatus.BAD_REQUEST));
    }



}
