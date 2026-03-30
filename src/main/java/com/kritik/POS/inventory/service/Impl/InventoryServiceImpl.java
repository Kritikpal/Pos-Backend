package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.inventory.entity.ItemStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.entity.MenuItemIngredient;
import com.kritik.POS.restaurant.mapper.RestaurantDtoMapper;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.models.request.StockUpdateRequest;
import com.kritik.POS.restaurant.models.response.StockReport;
import com.kritik.POS.restaurant.models.response.StockResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
    private final StockRepository stockRepository;
    private final IngredientStockRepository ingredientStockRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final TenantAccessService tenantAccessService;
    private final RestaurantDtoMapper restaurantDtoMapper;
    private final InventoryUtil inventoryUtil;

    @Override
    public StockReport getStockReport(String sku) {
        return StockReport.buildStockReport(inventoryUtil.getAccessibleStock(sku));
    }

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
    public void applyStockChangesForCompletedOrder(String orderId) {
        Order order = orderRepository.findByOrderIdWithItems(orderId)
                .orElseThrow(() -> new AppException("Order not found", HttpStatus.BAD_REQUEST));
        if (order.getPaymentStatus() != PaymentStatus.PAYMENT_SUCCESSFUL) {
            return;
        }

        List<ItemStock> itemStocks = new ArrayList<>();
        List<IngredientStock> ingredientStocks = new ArrayList<>();
        List<MenuItem> updatedMenus = new ArrayList<>();
        Set<String> updatedStockSkus = new HashSet<>();
        Set<String> updatedIngredientSkus = new HashSet<>();
        Set<Long> updatedMenuIds = new HashSet<>();

        for (SaleItem saleItem : order.getOrderItemList()) {
            MenuItem menuItem = saleItem.getMenuItem();
            if (menuItem == null) {
                continue;
            }
            if (InventoryAvailabilityUtil.hasRecipe(menuItem)) {
                for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
                    IngredientStock ingredientStock = ingredientUsage.getIngredientStock();
                    ingredientStock.setTotalStock(ingredientStock.getTotalStock() - (ingredientUsage.getQuantityRequired() * saleItem.getAmount()));
                    if (updatedIngredientSkus.add(ingredientStock.getSku())) {
                        ingredientStocks.add(ingredientStock);
                    }
                }
                InventoryUtil.syncMenuAvailability(menuItem);
                if (updatedMenuIds.add(menuItem.getId())) {
                    updatedMenus.add(menuItem);
                }
                continue;
            }

            ItemStock itemStock = menuItem.getItemStock();
            if (itemStock == null) {
                throw new StockException(menuItem.getItemName() + " does not have an inventory record.");
            }
            itemStock.setTotalStock(itemStock.getTotalStock() - saleItem.getAmount());
            InventoryUtil.syncMenuAvailability(itemStock);
            if (updatedStockSkus.add(itemStock.getSku())) {
                itemStocks.add(itemStock);
            }
            if (updatedMenuIds.add(menuItem.getId())) {
                updatedMenus.add(menuItem);
            }
        }

        stockRepository.saveAll(itemStocks);
        ingredientStockRepository.saveAll(ingredientStocks);
        menuItemRepository.saveAll(updatedMenus);
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
