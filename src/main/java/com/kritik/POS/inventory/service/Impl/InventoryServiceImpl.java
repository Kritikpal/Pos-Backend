package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.inventory.api.StockRequest;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.request.StockUpdateRequest;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.StockReport;
import com.kritik.POS.inventory.models.response.StockResponse;
import com.kritik.POS.inventory.projection.PreparedStockDeductionProjection;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.response.DirectStockDeductionProjection;
import com.kritik.POS.order.model.response.IngredientStockDeductionProjection;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.enums.MenuStockStrategy;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.MenuItemIngredientRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.restaurant.specification.MenuItemSpecification;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
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
import java.util.*;
import java.util.stream.Collectors;

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
    private final PreparedItemStockRepository preparedItemStockRepository;


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
    public StockResponse getStockBySku(String sku) {
        return StockResponse.fromEntity(inventoryUtil.getAccessibleStock(sku));
    }

    @Override
    @Transactional
    public StockResponse saveStock(ItemStockUpsertRequest itemStockUpsertRequest) {
        MenuItem menuItem = getAccessibleMenuItem(itemStockUpsertRequest.menuItemId());
        MenuStockStrategy stockStrategy = InventoryAvailabilityUtil.resolveStockStrategy(menuItem);
        if (stockStrategy != MenuStockStrategy.DIRECT) {
            throw new AppException("Only direct menu items can manage stock from this endpoint", HttpStatus.BAD_REQUEST);
        }

        ItemStock itemStock = menuItem.getItemStock() == null ? new ItemStock() : menuItem.getItemStock();
        if (itemStock.getSku() == null) {
            itemStock.setSku(UUID.randomUUID().toString());
            itemStock.setMenuItem(menuItem);
            menuItem.setItemStock(itemStock);
        }

        itemStock.setRestaurantId(menuItem.getRestaurantId());
        itemStock.setTotalStock(itemStock.getTotalStock() == null ? 0 : itemStock.getTotalStock());
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

        InventoryUtil.syncMenuAvailability(menuItem);
        return StockResponse.fromEntity(stockRepository.save(itemStock));
    }

    @Override
    @Transactional
    public StockResponse updateStock(String sku, StockUpdateRequest stockUpdateRequest) {
        ItemStock itemStock = inventoryUtil.getAccessibleStock(sku);
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
        List<DirectStockDeductionProjection> directStockDeductions = saleItemRepository.findDirectStockDeductionsByOrderId(orderId);
        List<IngredientStockDeductionProjection> ingredientStockDeductions = saleItemRepository.findIngredientStockDeductionsByOrderId(orderId);
        List<PreparedStockDeductionProjection> preparedStockDeductions = preparedItemStockRepository.findPreparedStockDeductionsByOrderId(orderId);
        deductStockForRequirements(
                directStockDeductions.stream()
                        .map(deduction -> new StockRequest(deduction.getSku(), deduction.getQuantity().intValue()))
                        .toList(),
                ingredientStockDeductions.stream()
                        .collect(Collectors.toMap(
                                IngredientStockDeductionProjection::getSku,
                                IngredientStockDeductionProjection::getQuantity,
                                Double::sum,
                                LinkedHashMap::new
                        )),
                preparedStockDeductions.stream()
                        .collect(Collectors.toMap(
                                PreparedStockDeductionProjection::getMenuItemId,
                                PreparedStockDeductionProjection::getQuantity,
                                Double::sum,
                                LinkedHashMap::new
                        )),
                mergeMenuIds(
                        saleItemRepository.findDistinctDirectMenuIdsByOrderId(orderId),
                        saleItemRepository.findDistinctPreparedMenuIdsByOrderId(orderId)
                )
        );
    }

    @Override
    @Transactional
    public void restoreStockForRefund(Order order) {

        if (order == null || order.getOrderId() == null) {
            throw new AppException("Order not found", HttpStatus.BAD_REQUEST);
        }
        String orderId = order.getOrderId();
        List<DirectStockDeductionProjection> directStockDeductions = saleItemRepository.findDirectStockDeductionsByOrderId(orderId);
        List<IngredientStockDeductionProjection> ingredientStockDeductions = saleItemRepository.findIngredientStockDeductionsByOrderId(orderId);
        List<PreparedStockDeductionProjection> preparedStockDeductions = preparedItemStockRepository.findPreparedStockDeductionsByOrderId(orderId);
        restoreStockForRequirements(
                directStockDeductions.stream()
                        .map(deduction -> new StockRequest(deduction.getSku(), deduction.getQuantity().intValue()))
                        .toList(),
                ingredientStockDeductions.stream()
                        .collect(Collectors.toMap(
                                IngredientStockDeductionProjection::getSku,
                                IngredientStockDeductionProjection::getQuantity,
                                Double::sum,
                                LinkedHashMap::new
                        )),
                preparedStockDeductions.stream()
                        .collect(Collectors.toMap(
                                PreparedStockDeductionProjection::getMenuItemId,
                                PreparedStockDeductionProjection::getQuantity,
                                Double::sum,
                                LinkedHashMap::new
                        )),
                mergeMenuIds(
                        saleItemRepository.findDistinctDirectMenuIdsByOrderId(orderId),
                        saleItemRepository.findDistinctPreparedMenuIdsByOrderId(orderId)
                )
        );
    }

    @Override
    @Transactional
    public void deductStockForRequirements(List<StockRequest> stockRequestList,
                                           Map<String, Double> ingredientRequirements,
                                           Map<Long, Double> preparedRequirements,
                                           Collection<Long> affectedMenuIds) {
        LocalDateTime updatedAt = LocalDateTime.now();

        if (stockRequestList != null) {
            for (StockRequest stockRequest : stockRequestList) {
                int updatedRows = stockRepository.deductStockQuantityIfAvailable(
                        stockRequest.sku(),
                        stockRequest.amount(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Insufficient stock for sku " + stockRequest.sku());
                }
            }
        }

        if (preparedRequirements != null) {
            for (Map.Entry<Long, Double> entry : preparedRequirements.entrySet()) {
                int updatedRows = preparedItemStockRepository.deductPreparedStockIfAvailable(
                        entry.getKey(),
                        entry.getValue(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Insufficient prepared stock for menu item " + entry.getKey());
                }
            }
        }

        if (ingredientRequirements != null) {
            for (Map.Entry<String, Double> entry : ingredientRequirements.entrySet()) {
                int updatedRows = ingredientStockRepository.deductStockQuantityIfAvailable(
                        entry.getKey(),
                        entry.getValue(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Insufficient ingredient stock for sku " + entry.getKey());
                }
            }
        }

        refreshMenuAvailability(
                affectedMenuIds,
                ingredientRequirements == null ? List.of() : ingredientRequirements.keySet()
        );
    }

    @Override
    @Transactional
    public void restoreStockForRequirements(List<StockRequest> stockRequestList,
                                            Map<String, Double> ingredientRequirements,
                                            Map<Long, Double> preparedRequirements,
                                            Collection<Long> affectedMenuIds) {
        LocalDateTime updatedAt = LocalDateTime.now();

        if (stockRequestList != null) {
            for (StockRequest stockRequest : stockRequestList) {
                int updatedRows = stockRepository.increaseStockQuantity(
                        stockRequest.sku(),
                        stockRequest.amount(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Stock not found for sku " + stockRequest.sku());
                }
            }
        }

        if (preparedRequirements != null) {
            for (Map.Entry<Long, Double> entry : preparedRequirements.entrySet()) {
                int updatedRows = preparedItemStockRepository.increasePreparedStock(
                        entry.getKey(),
                        entry.getValue(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Prepared stock not found for menu item " + entry.getKey());
                }
            }
        }

        if (ingredientRequirements != null) {
            for (Map.Entry<String, Double> entry : ingredientRequirements.entrySet()) {
                int updatedRows = ingredientStockRepository.increaseStockQuantity(
                        entry.getKey(),
                        entry.getValue(),
                        updatedAt
                );
                if (updatedRows == 0) {
                    throw new StockException("Ingredient stock not found for sku " + entry.getKey());
                }
            }
        }

        refreshMenuAvailability(
                affectedMenuIds,
                ingredientRequirements == null ? List.of() : ingredientRequirements.keySet()
        );
    }

    @Override
    public void checkOrderStockAvailability(List<StockRequest> stockRequestList,
                                            Map<String, Double> ingredientRequirements,
                                            Map<Long, Double> preparedRequirements) {
        checkStockAvailable(stockRequestList);
        checkIngredientStockAvailable(ingredientRequirements);
        checkPreparedStockAvailable(preparedRequirements);
    }

    @Override
    @Transactional
    public void refreshMenuAvailability(Collection<Long> menuIds, Collection<String> ingredientSkus) {
        LinkedHashSet<Long> affectedMenuIds = new LinkedHashSet<>();
        if (menuIds != null) {
            affectedMenuIds.addAll(menuIds);
        }
        if (ingredientSkus != null && !ingredientSkus.isEmpty()) {
            affectedMenuIds.addAll(menuItemIngredientRepository.findDistinctMenuIdsByIngredientStockSkuIn(ingredientSkus));
        }
        if (affectedMenuIds.isEmpty()) {
            return;
        }

        List<MenuItem> affectedMenus = menuItemRepository.findAllByIdIn(affectedMenuIds);
        for (MenuItem menuItem : affectedMenus) {
            InventoryUtil.syncMenuAvailability(menuItem);
        }
        menuItemRepository.saveAll(affectedMenus);
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

    private Collection<Long> mergeMenuIds(Collection<Long> firstIds, Collection<Long> secondIds) {
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();
        if (firstIds != null) {
            mergedIds.addAll(firstIds);
        }
        if (secondIds != null) {
            mergedIds.addAll(secondIds);
        }
        return mergedIds;
    }

    private void checkIngredientStockAvailable(Map<String, Double> ingredientRequirements) {
        if (ingredientRequirements == null || ingredientRequirements.isEmpty()) {
            return;
        }
        List<IngredientStock> ingredients = ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(ingredientRequirements.keySet());
        Map<String, IngredientStock> ingredientMap = new HashMap<>();
        for (IngredientStock ingredient : ingredients) {
            if (!tenantAccessService.isSuperAdmin()) {
                tenantAccessService.resolveAccessibleRestaurantId(ingredient.getRestaurantId());
            }
            ingredientMap.put(ingredient.getSku(), ingredient);
        }

        for (Map.Entry<String, Double> entry : ingredientRequirements.entrySet()) {
            IngredientStock ingredient = ingredientMap.get(entry.getKey());
            if (ingredient == null) {
                throw new StockException("Ingredient stock not found");
            }
            if (!Boolean.TRUE.equals(ingredient.getIsActive())) {
                throw new StockException(ingredient.getIngredientName() + " stock is inactive.");
            }
            if (ingredient.getTotalStock() == null || ingredient.getTotalStock() < entry.getValue()) {
                throw new StockException(ingredient.getIngredientName() + " is not available in stock only " + ingredient.getTotalStock() + " left.");
            }
        }
    }

    private void checkPreparedStockAvailable(Map<Long, Double> preparedRequirements) {
        if (preparedRequirements == null || preparedRequirements.isEmpty()) {
            return;
        }
        List<PreparedItemStock> preparedStocks = preparedItemStockRepository.findAllById(preparedRequirements.keySet());
        Map<Long, PreparedItemStock> preparedStockMap = new HashMap<>();
        for (PreparedItemStock preparedStock : preparedStocks) {
            if (!tenantAccessService.isSuperAdmin()) {
                tenantAccessService.resolveAccessibleRestaurantId(preparedStock.getRestaurantId());
            }
            preparedStockMap.put(preparedStock.getMenuItemId(), preparedStock);
        }

        for (Map.Entry<Long, Double> entry : preparedRequirements.entrySet()) {
            PreparedItemStock preparedStock = preparedStockMap.get(entry.getKey());
            if (preparedStock == null) {
                throw new StockException("Prepared stock not found for menu item " + entry.getKey());
            }
            if (!Boolean.TRUE.equals(preparedStock.getActive())) {
                throw new StockException("Prepared stock is inactive for menu item " + entry.getKey());
            }
            double availableQty = preparedStock.getAvailableQty() == null ? 0.0 : preparedStock.getAvailableQty();
            double reservedQty = preparedStock.getReservedQty() == null ? 0.0 : preparedStock.getReservedQty();
            if ((availableQty - reservedQty) < entry.getValue()) {
                throw new StockException("Prepared stock is not available for menu item " + entry.getKey());
            }
        }
    }


}
