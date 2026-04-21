package com.kritik.POS.mobile.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.mobile.dto.request.PosBootstrapRequest;
import com.kritik.POS.mobile.dto.request.PosPullRequest;
import com.kritik.POS.mobile.dto.request.SyncTimeCursorBundle;
import com.kritik.POS.mobile.dto.request.TimeCursorDto;
import com.kritik.POS.mobile.repository.PosSyncRepository;
import com.kritik.POS.mobile.repository.row.CategorySyncRow;
import com.kritik.POS.mobile.repository.row.ItemStockSyncRow;
import com.kritik.POS.mobile.repository.row.MenuItemSyncRow;
import com.kritik.POS.mobile.repository.row.TaxConfigSyncRow;
import com.kritik.POS.security.service.TenantAccessService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosSyncServiceImplTest {

    @Mock
    private PosSyncRepository posSyncRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private PosSyncServiceImpl posSyncService;

    @Test
    void bootstrapBuildsChangesDeletionsAndNextCursorFromDefaultCursorState() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 1, 1, 10, 0);
        PosBootstrapRequest request = new PosBootstrapRequest();
        request.setRestaurantId(11L);
        request.setDeviceId("device-1");
        request.setRequestedGroups(List.of("categories", "menuItems"));

        when(tenantAccessService.resolveAccessibleRestaurantId(11L)).thenReturn(11L);
        when(posSyncRepository.findCategoryRows(anyLong(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(List.of(
                        new CategorySyncRow(1L, 11L, "Category One", "Desc", true, false, syncTime, syncTime, syncTime),
                        new CategorySyncRow(2L, 11L, "Deleted Category", "Desc", false, true, syncTime.plusMinutes(1), syncTime.plusMinutes(1), syncTime.plusMinutes(1))
                ));
        when(posSyncRepository.findMenuItemRows(anyLong(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(List.of(
                        new MenuItemSyncRow(21L, 11L, 1L, 31L, "uploads/menu.png", "Menu One", "Desc", true, true, false, false, false, false, syncTime, syncTime, syncTime)
                ));

        var response = posSyncService.bootstrap(request);

        assertThat(response.getChanges().getCategories()).hasSize(1);
        assertThat(response.getDeletions().getCategories()).containsExactly(2L);
        assertThat(response.getChanges().getMenuItems()).singleElement().satisfies(menuItem -> {
            assertThat(menuItem.menuItemId()).isEqualTo(21L);
            assertThat(menuItem.recipeBased()).isFalse();
            assertThat(menuItem.productImageUrl()).isEqualTo("/uploads/menu.png");
        });
        assertThat(response.getNextCursors().getCategories().getLastSeenKey()).isEqualTo("2");
        assertThat(response.isHasMore()).isFalse();
    }

    @Test
    void pullOnlyLoadsRequestedGroups() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 1, 2, 10, 0);
        PosPullRequest request = new PosPullRequest();
        request.setRestaurantId(12L);
        request.setDeviceId("device-2");
        request.setRequestedGroups(List.of("taxes"));
        request.setCursors(new SyncTimeCursorBundle());

        when(tenantAccessService.resolveAccessibleRestaurantId(12L)).thenReturn(12L);
        when(posSyncRepository.findTaxRows(anyLong(), any(), anyLong(), any(Pageable.class)))
                .thenReturn(List.of(new TaxConfigSyncRow(7L, 12L, "GST", 5.0, true, false, syncTime, syncTime, syncTime)));

        var response = posSyncService.pull(request);

        assertThat(response.getChanges().getTaxes()).hasSize(1);
        verify(posSyncRepository).findTaxRows(anyLong(), any(), anyLong(), any(Pageable.class));
        verify(posSyncRepository, never()).findCategoryRows(anyLong(), any(), anyLong(), any(Pageable.class));
        verify(posSyncRepository, never()).findMenuItemRows(anyLong(), any(), anyLong(), any(Pageable.class));
        verify(posSyncRepository, never()).findItemStockRows(anyLong(), any(), anyString(), any(Pageable.class));
    }

    @Test
    void pullRejectsUnknownRequestedGroup() {
        PosPullRequest request = new PosPullRequest();
        request.setRestaurantId(15L);
        request.setDeviceId("device-3");
        request.setRequestedGroups(List.of("unknownGroup"));
        request.setCursors(new SyncTimeCursorBundle());

        when(tenantAccessService.resolveAccessibleRestaurantId(15L)).thenReturn(15L);

        assertThatThrownBy(() -> posSyncService.pull(request))
                .isInstanceOf(com.kritik.POS.exception.errors.BadRequestException.class)
                .hasMessageContaining("Unsupported requested group");

        verifyNoMoreInteractions(posSyncRepository);
    }

    @Test
    void pullPropagatesRestaurantAccessFailure() {
        PosPullRequest request = new PosPullRequest();
        request.setRestaurantId(18L);
        request.setDeviceId("device-4");
        request.setRequestedGroups(List.of("categories"));
        request.setCursors(new SyncTimeCursorBundle());

        when(tenantAccessService.resolveAccessibleRestaurantId(18L))
                .thenThrow(new AppException("Restaurant access denied", HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> posSyncService.pull(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Restaurant access denied");
    }

    @Test
    void pullParsesNumericAndStringCursorKeysBeforeRepositoryCalls() {
        LocalDateTime syncTime = LocalDateTime.of(2026, 1, 3, 10, 0);
        SyncTimeCursorBundle cursors = new SyncTimeCursorBundle();
        cursors.setCategories(new TimeCursorDto(syncTime.toInstant(ZoneOffset.UTC), "44"));
        cursors.setItemStocks(new TimeCursorDto(syncTime.toInstant(ZoneOffset.UTC), "STOCK-B"));

        PosPullRequest request = new PosPullRequest();
        request.setRestaurantId(20L);
        request.setDeviceId("device-5");
        request.setRequestedGroups(List.of("categories", "itemStocks"));
        request.setCursors(cursors);

        when(tenantAccessService.resolveAccessibleRestaurantId(20L)).thenReturn(20L);
        when(posSyncRepository.findCategoryRows(eq(20L), eq(syncTime), eq(44L), argThat(pageable ->
                pageable != null && pageable.getPageNumber() == 0 && pageable.getPageSize() == 501
        )))
                .thenReturn(List.of());
        when(posSyncRepository.findItemStockRows(eq(20L), eq(syncTime), eq("STOCK-B"), argThat(pageable ->
                pageable != null && pageable.getPageNumber() == 0 && pageable.getPageSize() == 501
        )))
                .thenReturn(List.of(new ItemStockSyncRow("STOCK-C", 20L, 1L, null, null, 5, 1, "pcs", true, false, syncTime, syncTime, syncTime)));

        var response = posSyncService.pull(request);

        assertThat(response.getChanges().getItemStocks()).extracting(item -> item.sku()).containsExactly("STOCK-C");
        verify(posSyncRepository).findCategoryRows(eq(20L), eq(syncTime), eq(44L), argThat(pageable ->
                pageable != null && pageable.getPageNumber() == 0 && pageable.getPageSize() == 501
        ));
        verify(posSyncRepository).findItemStockRows(eq(20L), eq(syncTime), eq("STOCK-B"), argThat(pageable ->
                pageable != null && pageable.getPageNumber() == 0 && pageable.getPageSize() == 501
        ));
    }
}
