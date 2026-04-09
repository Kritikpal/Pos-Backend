package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.StockException;
import com.kritik.POS.inventory.entity.enums.MenuStockStrategy;
import com.kritik.POS.inventory.entity.production.ProductionEntry;
import com.kritik.POS.inventory.entity.production.ProductionEntryItem;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.models.request.ProductionEntryCreateRequest;
import com.kritik.POS.inventory.models.response.ProductionEntryItemResponseDto;
import com.kritik.POS.inventory.models.response.ProductionEntryResponseDto;
import com.kritik.POS.inventory.models.response.ProductionEntrySummaryDto;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.repository.ProductionEntryItemRepository;
import com.kritik.POS.inventory.repository.ProductionEntryRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.service.ProductionEntryService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductionEntryServiceImpl implements ProductionEntryService {

    private final ProductionEntryRepository productionEntryRepository;
    private final ProductionEntryItemRepository productionEntryItemRepository;
    private final IngredientStockRepository ingredientStockRepository;
    private final PreparedItemStockRepository preparedItemStockRepository;
    private final InventoryService inventoryService;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public ProductionEntryResponseDto createProductionEntry(ProductionEntryCreateRequest request) {
        MenuItem menuItem = inventoryService.getAccessibleMenuItem(request.menuItemId());
        if (InventoryAvailabilityUtil.resolveStockStrategy(menuItem) != MenuStockStrategy.PREPARED) {
            throw new AppException("Production entry is only supported for prepared menu items", HttpStatus.BAD_REQUEST);
        }

        Long restaurantId = tenantAccessService.resolveManageableRestaurantId(
                request.restaurantId() != null ? request.restaurantId() : menuItem.getRestaurantId()
        );
        if (!restaurantId.equals(menuItem.getRestaurantId())) {
            throw new AppException("Menu item does not belong to the selected restaurant", HttpStatus.BAD_REQUEST);
        }

        Map<String, Double> ingredientRequirements = new LinkedHashMap<>();
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            ingredientRequirements.merge(
                    ingredientUsage.getIngredientStock().getSku(),
                    InventoryAvailabilityUtil.computeRequiredIngredientQuantity(ingredientUsage, request.producedQty()),
                    Double::sum
            );
        }
        inventoryService.checkOrderStockAvailability(List.of(), ingredientRequirements, Map.<Long, Double>of());

        ProductionEntry entry = new ProductionEntry();
        entry.setRestaurantId(restaurantId);
        entry.setMenuItemId(menuItem.getId());
        entry.setProducedQty(request.producedQty());
        entry.setUnitCode(request.unitCode().trim());
        entry.setProductionTime(request.productionTime());
        entry.setNotes(InventoryUtil.trimToNull(request.notes()));
        entry.setCreatedBy(resolveCurrentUserId());
        ProductionEntry savedEntry = productionEntryRepository.save(entry);

        List<ProductionEntryItem> entryItems = new ArrayList<>();
        for (MenuItemIngredient ingredientUsage : menuItem.getIngredientUsages()) {
            Double deductedQty = InventoryAvailabilityUtil.computeRequiredIngredientQuantity(ingredientUsage, request.producedQty());
            int updatedRows = ingredientStockRepository.deductStockQuantityIfAvailable(
                    ingredientUsage.getIngredientStock().getSku(),
                    deductedQty,
                    savedEntry.getCreatedAt()
            );
            if (updatedRows == 0) {
                throw new StockException("Insufficient ingredient stock for sku " + ingredientUsage.getIngredientStock().getSku());
            }

            ProductionEntryItem entryItem = new ProductionEntryItem();
            entryItem.setProductionEntry(savedEntry);
            entryItem.setIngredientSku(ingredientUsage.getIngredientStock().getSku());
            entryItem.setIngredientName(ingredientUsage.getIngredientStock().getIngredientName());
            entryItem.setDeductedQty(deductedQty);
            entryItem.setUnitCode(ingredientUsage.getIngredientStock().getUnitOfMeasure());
            entryItems.add(entryItem);
        }
        productionEntryItemRepository.saveAll(entryItems);

        PreparedItemStock preparedItemStock = preparedItemStockRepository.findById(menuItem.getId())
                .orElseGet(PreparedItemStock::new);
        preparedItemStock.setMenuItemId(menuItem.getId());
        preparedItemStock.setRestaurantId(menuItem.getRestaurantId());
        preparedItemStock.setUnitCode(request.unitCode().trim());
        preparedItemStock.setActive(Boolean.TRUE.equals(menuItem.getIsActive()) && !Boolean.TRUE.equals(menuItem.getIsDeleted()));
        double currentAvailableQty = preparedItemStock.getAvailableQty() == null ? 0.0 : preparedItemStock.getAvailableQty();
        preparedItemStock.setAvailableQty(currentAvailableQty + request.producedQty());
        if (preparedItemStock.getReservedQty() == null) {
            preparedItemStock.setReservedQty(0.0);
        }
        PreparedItemStock savedPreparedStock = preparedItemStockRepository.save(preparedItemStock);

        inventoryService.refreshMenuAvailability(List.of(menuItem.getId()), ingredientRequirements.keySet());
        return toResponse(savedEntry, menuItem.getItemName(), savedPreparedStock, entryItems);
    }

    @Override
    public PageResponse<ProductionEntrySummaryDto> getProductionEntryPage(Long chainId,
                                                                          Long restaurantId,
                                                                          Long menuItemId,
                                                                          Integer pageNumber,
                                                                          Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }

        return PageResponse.from(
                productionEntryRepository.findSummaries(
                                tenantAccessService.isSuperAdmin(),
                                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                                menuItemId,
                                PageRequest.of(pageNumber, pageSize)
                        )
                        .map(ProductionEntrySummaryDto::fromProjection)
        );
    }

    @Override
    public ProductionEntryResponseDto getProductionEntry(Long id) {
        ProductionEntry entry = getAccessibleEntry(id);
        MenuItem menuItem = inventoryService.getAccessibleMenuItem(entry.getMenuItemId());
        PreparedItemStock preparedItemStock = preparedItemStockRepository.findById(entry.getMenuItemId()).orElse(null);
        List<ProductionEntryItem> items = productionEntryItemRepository.findAllByProductionEntry_IdOrderByIdAsc(entry.getId());
        return toResponse(entry, menuItem.getItemName(), preparedItemStock, items);
    }

    private ProductionEntry getAccessibleEntry(Long id) {
        if (tenantAccessService.isSuperAdmin()) {
            return productionEntryRepository.findById(id)
                    .orElseThrow(() -> new AppException("Production entry not found", HttpStatus.BAD_REQUEST));
        }
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        return productionEntryRepository.findByIdAndRestaurantIdIn(id, accessibleRestaurantIds)
                .orElseThrow(() -> new AppException("Production entry not found", HttpStatus.BAD_REQUEST));
    }

    private ProductionEntryResponseDto toResponse(ProductionEntry entry,
                                                  String menuItemName,
                                                  PreparedItemStock preparedItemStock,
                                                  List<ProductionEntryItem> items) {
        double netPreparedQty = 0.0;
        if (preparedItemStock != null && Boolean.TRUE.equals(preparedItemStock.getActive())) {
            double availableQty = preparedItemStock.getAvailableQty() == null ? 0.0 : preparedItemStock.getAvailableQty();
            double reservedQty = preparedItemStock.getReservedQty() == null ? 0.0 : preparedItemStock.getReservedQty();
            netPreparedQty = Math.max(availableQty - reservedQty, 0.0);
        }

        return new ProductionEntryResponseDto(
                entry.getId(),
                entry.getRestaurantId(),
                entry.getMenuItemId(),
                menuItemName,
                entry.getProducedQty(),
                entry.getUnitCode(),
                entry.getProductionTime(),
                entry.getNotes(),
                entry.getCreatedBy(),
                entry.getCreatedAt(),
                netPreparedQty,
                items.stream()
                        .map(item -> new ProductionEntryItemResponseDto(
                                item.getIngredientSku(),
                                item.getIngredientName(),
                                item.getDeductedQty(),
                                item.getUnitCode()
                        ))
                        .toList()
        );
    }

    private Long resolveCurrentUserId() {
        try {
            SecurityUser currentUser = tenantAccessService.currentUser();
            return currentUser == null ? null : currentUser.getUserId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

}
