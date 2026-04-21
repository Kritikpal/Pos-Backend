package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.enums.MenuStockStrategy;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.models.request.PreparedStockUpdateRequest;
import com.kritik.POS.inventory.models.response.PreparedStockResponseDto;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.service.PreparedStockService;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PreparedStockServiceImpl implements PreparedStockService {

    private final PreparedItemStockRepository preparedItemStockRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryService inventoryService;
    private final TenantAccessService tenantAccessService;

    @Override
    public PageResponse<PreparedStockResponseDto> getPreparedStockPage(Long chainId,
                                                                       Long restaurantId,
                                                                       String search,
                                                                       Integer pageNumber,
                                                                       Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }

        Page<PreparedStockResponseDto> page = preparedItemStockRepository.findPreparedStockSummaries(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        InventoryUtil.normalizeSearch(search),
                        PageRequest.of(pageNumber, pageSize)
                )
                .map(PreparedStockResponseDto::fromProjection);
        return PageResponse.from(page);
    }

    @Override
    public PreparedStockResponseDto getPreparedStock(Long menuItemId) {
        return PreparedStockResponseDto.fromMenuItem(loadDetailedPreparedMenu(menuItemId, resolveAccessibleRestaurantIds()));
    }

    @Override
    @Transactional
    public PreparedStockResponseDto updatePreparedStock(Long menuItemId, PreparedStockUpdateRequest request) {
        MenuItem menuItem = inventoryService.getAccessibleMenuItem(menuItemId);
        if (InventoryAvailabilityUtil.resolveStockStrategy(menuItem) != MenuStockStrategy.PREPARED) {
            throw new AppException("Prepared stock is only supported for prepared menu items", HttpStatus.BAD_REQUEST);
        }
        tenantAccessService.resolveManageableRestaurantId(menuItem.getRestaurantId());

        PreparedItemStock preparedItemStock = preparedItemStockRepository.findById(menuItemId)
                .orElseGet(PreparedItemStock::new);
        preparedItemStock.setMenuItemId(menuItemId);
        preparedItemStock.setRestaurantId(menuItem.getRestaurantId());
        if (preparedItemStock.getAvailableQty() == null) {
            preparedItemStock.setAvailableQty(0.0);
        }
        if (preparedItemStock.getReservedQty() == null) {
            preparedItemStock.setReservedQty(0.0);
        }
        if (request.unitCode() != null && !request.unitCode().isBlank()) {
            preparedItemStock.setUnitCode(request.unitCode().trim());
        } else if (preparedItemStock.getUnitCode() == null || preparedItemStock.getUnitCode().isBlank()) {
            preparedItemStock.setUnitCode("serving");
        }
        if (request.isActive() != null) {
            preparedItemStock.setActive(request.isActive());
        } else if (preparedItemStock.getActive() == null) {
            preparedItemStock.setActive(Boolean.TRUE.equals(menuItem.getIsActive()) && !Boolean.TRUE.equals(menuItem.getIsDeleted()));
        }

        preparedItemStockRepository.save(preparedItemStock);
        inventoryService.refreshMenuAvailability(List.of(menuItemId), List.of());
        return getPreparedStock(menuItemId);
    }

    private MenuItem loadDetailedPreparedMenu(Long menuItemId, Collection<Long> accessibleRestaurantIds) {
        MenuItem menuItem = menuItemRepository.findDetailedById(
                        menuItemId,
                        tenantAccessService.isSuperAdmin(),
                        accessibleRestaurantIds
                )
                .orElseThrow(() -> new AppException("Prepared stock not found", HttpStatus.BAD_REQUEST));
        if (InventoryAvailabilityUtil.resolveStockStrategy(menuItem) != MenuStockStrategy.PREPARED) {
            throw new AppException("Prepared stock not found", HttpStatus.BAD_REQUEST);
        }
        return menuItem;
    }

    private Collection<Long> resolveAccessibleRestaurantIds() {
        return tenantAccessService.resolveAccessibleRestaurantIds(null, null);
    }
}
