package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.models.request.PreparedStockUpdateRequest;
import com.kritik.POS.inventory.models.response.PreparedStockResponseDto;
import com.kritik.POS.inventory.projection.PreparedStockSummaryProjection;
import com.kritik.POS.inventory.repository.PreparedItemStockRepository;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreparedStockServiceImplTest {

    @Mock
    private PreparedItemStockRepository preparedItemStockRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private PreparedStockServiceImpl preparedStockService;

    @Test
    void getPreparedStockPageMapsPreparedStockRows() {
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, 10L)).thenReturn(List.of(10L));
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(List.of(10L))).thenReturn(List.of(10L));
        when(preparedItemStockRepository.findPreparedStockSummaries(false, List.of(10L), "paneer", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(summaryProjection())));

        PageResponse<PreparedStockResponseDto> page = preparedStockService.getPreparedStockPage(null, 10L, "paneer", 0, 20);

        assertThat(page.items()).hasSize(1);
        assertThat(page.items().get(0).menuItemId()).isEqualTo(200L);
        assertThat(page.items().get(0).netAvailableQty()).isEqualTo(6.0);
    }

    @Test
    void updatePreparedStockChangesOnlyMetadata() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(200L);
        menuItem.setRestaurantId(10L);
        menuItem.setItemName("Paneer Roll");
        menuItem.setMenuType(MenuType.PREPARED);
        menuItem.setIsActive(true);
        menuItem.setIsDeleted(false);

        PreparedItemStock preparedItemStock = new PreparedItemStock();
        preparedItemStock.setMenuItemId(200L);
        preparedItemStock.setRestaurantId(10L);
        preparedItemStock.setAvailableQty(8.0);
        preparedItemStock.setReservedQty(2.0);
        preparedItemStock.setUnitCode("PCS");
        preparedItemStock.setActive(true);
        preparedItemStock.setUpdatedAt(LocalDateTime.of(2026, 4, 19, 12, 0));

        MenuItem detailedMenu = new MenuItem();
        detailedMenu.setId(200L);
        detailedMenu.setRestaurantId(10L);
        detailedMenu.setItemName("Paneer Roll");
        detailedMenu.setMenuType(MenuType.PREPARED);
        detailedMenu.setIsActive(true);
        detailedMenu.setIsDeleted(false);
        detailedMenu.setPreparedItemStock(preparedItemStock);

        when(inventoryService.getAccessibleMenuItem(200L)).thenReturn(menuItem);
        when(tenantAccessService.resolveManageableRestaurantId(10L)).thenReturn(10L);
        when(preparedItemStockRepository.findById(200L)).thenReturn(Optional.of(preparedItemStock));
        when(preparedItemStockRepository.save(any(PreparedItemStock.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantAccessService.resolveAccessibleRestaurantIds(null, null)).thenReturn(List.of(10L));
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(menuItemRepository.findDetailedById(eq(200L), eq(false), eq(List.of(10L)))).thenReturn(Optional.of(detailedMenu));

        PreparedStockResponseDto response = preparedStockService.updatePreparedStock(
                200L,
                new PreparedStockUpdateRequest("SERVING", false)
        );

        assertThat(preparedItemStock.getAvailableQty()).isEqualTo(8.0);
        assertThat(preparedItemStock.getReservedQty()).isEqualTo(2.0);
        assertThat(preparedItemStock.getUnitCode()).isEqualTo("SERVING");
        assertThat(preparedItemStock.getActive()).isFalse();
        assertThat(response.availableQty()).isEqualTo(8.0);
        assertThat(response.reservedQty()).isEqualTo(2.0);
        verify(inventoryService).refreshMenuAvailability(List.of(200L), List.of());
    }

    private PreparedStockSummaryProjection summaryProjection() {
        return new PreparedStockSummaryProjection() {
            @Override
            public Long getMenuItemId() {
                return 200L;
            }

            @Override
            public Long getRestaurantId() {
                return 10L;
            }

            @Override
            public String getItemName() {
                return "Paneer Roll";
            }

            @Override
            public String getImage() {
                return null;
            }

            @Override
            public String getUnitCode() {
                return "PCS";
            }

            @Override
            public Double getAvailableQty() {
                return 8.0;
            }

            @Override
            public Double getReservedQty() {
                return 2.0;
            }

            @Override
            public Boolean getIsActive() {
                return true;
            }

            @Override
            public LocalDateTime getUpdatedAt() {
                return LocalDateTime.of(2026, 4, 19, 12, 0);
            }
        };
    }
}
