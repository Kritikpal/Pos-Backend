package com.kritik.POS.mobile.service;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.mobile.dto.request.PosBootstrapRequest;
import com.kritik.POS.mobile.dto.request.PosPullRequest;
import com.kritik.POS.mobile.dto.request.SyncTimeCursorBundle;
import com.kritik.POS.mobile.dto.request.TimeCursorDto;
import com.kritik.POS.mobile.dto.response.PosSyncResponse;
import com.kritik.POS.mobile.dto.response.SyncChanges;
import com.kritik.POS.mobile.dto.response.SyncDeletions;
import com.kritik.POS.mobile.dto.response.syncDtos.CategorySyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.IngredientStockSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.ItemStockSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.MenuItemSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.MenuPriceSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.MenuRecipeItemSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.MenuRecipeSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.PosSettingSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.PreparedStockSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.TaxClassSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.TaxConfigSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.TaxDefinitionSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.TaxRegistrationSyncDto;
import com.kritik.POS.mobile.dto.response.syncDtos.TaxRuleSyncDto;
import com.kritik.POS.mobile.repository.PosSyncRepository;
import com.kritik.POS.mobile.repository.row.CategorySyncRow;
import com.kritik.POS.mobile.repository.row.IngredientStockSyncRow;
import com.kritik.POS.mobile.repository.row.ItemStockSyncRow;
import com.kritik.POS.mobile.repository.row.MenuItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuPriceSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeSyncRow;
import com.kritik.POS.mobile.repository.row.PosSettingSyncRow;
import com.kritik.POS.mobile.repository.row.PreparedStockSyncRow;
import com.kritik.POS.mobile.repository.row.SyncStreamRow;
import com.kritik.POS.mobile.repository.row.TaxClassSyncRow;
import com.kritik.POS.mobile.repository.row.TaxConfigSyncRow;
import com.kritik.POS.mobile.repository.row.TaxDefinitionSyncRow;
import com.kritik.POS.mobile.repository.row.TaxRegistrationSyncRow;
import com.kritik.POS.mobile.repository.row.TombstoneSyncRow;
import com.kritik.POS.mobile.repository.row.TaxRuleSyncRow;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.security.service.TenantAccessService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PosSyncServiceImpl implements PosSyncService {

    private static final int DEFAULT_PAGE_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 500;
    private static final ZoneOffset CURSOR_ZONE = ZoneOffset.UTC;

    private final PosSyncRepository posSyncRepository;
    private final TenantAccessService tenantAccessService;

    @Override
    public PosSyncResponse bootstrap(PosBootstrapRequest request) {
        Long restaurantId = tenantAccessService.resolveAccessibleRestaurantId(request.getRestaurantId());
        return sync(
                restaurantId,
                request.getDeviceId(),
                request.getPageSize(),
                request.getRequestedGroups(),
                null
        );
    }

    @Override
    public PosSyncResponse pull(PosPullRequest request) {
        Long restaurantId = tenantAccessService.resolveAccessibleRestaurantId(request.getRestaurantId());
        return sync(
                restaurantId,
                request.getDeviceId(),
                request.getPageSize(),
                request.getRequestedGroups(),
                request.getCursors()
        );
    }

    private PosSyncResponse sync(Long restaurantId,
                                 String deviceId,
                                 Integer requestedPageSize,
                                 List<String> requestedGroups,
                                 SyncTimeCursorBundle incomingCursors) {
        int pageSize = resolvePageSize(requestedPageSize);
        Set<SyncGroup> groups = resolveRequestedGroups(requestedGroups);
        SyncTimeCursorBundle nextCursors = copyCursorBundle(incomingCursors);
        SyncChanges changes = SyncChanges.builder().build();
        SyncDeletions deletions = SyncDeletions.builder().build();
        boolean hasMore = false;

        if (groups.contains(SyncGroup.CATEGORIES)) {
            TombstoneSlice<CategorySyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findCategoryRows(
                            restaurantId,
                            cursorTime(nextCursors.getCategories()),
                            parseLongCursor(nextCursors.getCategories(), SyncGroup.CATEGORIES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getCategories(),
                    pageSize
            );
            changes.setCategories(slice.liveRows().stream().map(this::toCategoryDto).toList());
            deletions.setCategories(slice.deletedRows().stream().map(CategorySyncRow::categoryId).toList());
            nextCursors.setCategories(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.MENU_ITEMS)) {
            TombstoneSlice<MenuItemSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findMenuItemRows(
                            restaurantId,
                            cursorTime(nextCursors.getMenuItems()),
                            parseLongCursor(nextCursors.getMenuItems(), SyncGroup.MENU_ITEMS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getMenuItems(),
                    pageSize
            );
            changes.setMenuItems(slice.liveRows().stream().map(this::toMenuItemDto).toList());
            deletions.setMenuItems(slice.deletedRows().stream().map(MenuItemSyncRow::menuItemId).toList());
            nextCursors.setMenuItems(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.PRICES)) {
            SliceResult<MenuPriceSyncRow> slice = buildSlice(
                    posSyncRepository.findPriceRows(
                            restaurantId,
                            cursorTime(nextCursors.getPrices()),
                            parseLongCursor(nextCursors.getPrices(), SyncGroup.PRICES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getPrices(),
                    pageSize
            );
            changes.setPrices(slice.rows().stream().map(this::toPriceDto).toList());
            nextCursors.setPrices(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.TAXES)) {
            TombstoneSlice<TaxConfigSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findTaxRows(
                            restaurantId,
                            cursorTime(nextCursors.getTaxes()),
                            parseLongCursor(nextCursors.getTaxes(), SyncGroup.TAXES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getTaxes(),
                    pageSize
            );
            changes.setTaxes(slice.liveRows().stream().map(this::toTaxDto).toList());
            deletions.setTaxes(slice.deletedRows().stream().map(TaxConfigSyncRow::taxId).toList());
            nextCursors.setTaxes(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.TAX_CLASSES)) {
            TombstoneSlice<TaxClassSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findTaxClassRows(
                            restaurantId,
                            cursorTime(nextCursors.getTaxClasses()),
                            parseLongCursor(nextCursors.getTaxClasses(), SyncGroup.TAX_CLASSES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getTaxClasses(),
                    pageSize
            );
            changes.setTaxClasses(slice.liveRows().stream().map(this::toTaxClassDto).toList());
            deletions.setTaxClasses(slice.deletedRows().stream().map(TaxClassSyncRow::id).toList());
            nextCursors.setTaxClasses(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.TAX_DEFINITIONS)) {
            TombstoneSlice<TaxDefinitionSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findTaxDefinitionRows(
                            restaurantId,
                            cursorTime(nextCursors.getTaxDefinitions()),
                            parseLongCursor(nextCursors.getTaxDefinitions(), SyncGroup.TAX_DEFINITIONS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getTaxDefinitions(),
                    pageSize
            );
            changes.setTaxDefinitions(slice.liveRows().stream().map(this::toTaxDefinitionDto).toList());
            deletions.setTaxDefinitions(slice.deletedRows().stream().map(TaxDefinitionSyncRow::id).toList());
            nextCursors.setTaxDefinitions(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.TAX_RULES)) {
            TombstoneSlice<TaxRuleSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findTaxRuleRows(
                            restaurantId,
                            cursorTime(nextCursors.getTaxRules()),
                            parseLongCursor(nextCursors.getTaxRules(), SyncGroup.TAX_RULES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getTaxRules(),
                    pageSize
            );
            changes.setTaxRules(slice.liveRows().stream().map(this::toTaxRuleDto).toList());
            deletions.setTaxRules(slice.deletedRows().stream().map(TaxRuleSyncRow::id).toList());
            nextCursors.setTaxRules(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.TAX_REGISTRATIONS)) {
            SliceResult<TaxRegistrationSyncRow> slice = buildSlice(
                    posSyncRepository.findTaxRegistrationRows(
                            restaurantId,
                            cursorTime(nextCursors.getTaxRegistrations()),
                            parseLongCursor(nextCursors.getTaxRegistrations(), SyncGroup.TAX_REGISTRATIONS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getTaxRegistrations(),
                    pageSize
            );
            changes.setTaxRegistrations(slice.rows().stream().map(this::toTaxRegistrationDto).toList());
            nextCursors.setTaxRegistrations(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.ITEM_STOCK)) {
            TombstoneSlice<ItemStockSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findItemStockRows(
                            restaurantId,
                            cursorTime(nextCursors.getItemStocks()),
                            parseStringCursor(nextCursors.getItemStocks()),
                            firstPage(pageSize)
                    ),
                    nextCursors.getItemStocks(),
                    pageSize
            );
            changes.setItemStocks(slice.liveRows().stream().map(this::toItemStockDto).toList());
            deletions.setItemStocks(slice.deletedRows().stream().map(ItemStockSyncRow::sku).toList());
            nextCursors.setItemStocks(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.INGREDIENT_STOCK)) {
            TombstoneSlice<IngredientStockSyncRow> slice = buildTombstoneSlice(
                    posSyncRepository.findIngredientStockRows(
                            restaurantId,
                            cursorTime(nextCursors.getIngredientStocks()),
                            parseStringCursor(nextCursors.getIngredientStocks()),
                            firstPage(pageSize)
                    ),
                    nextCursors.getIngredientStocks(),
                    pageSize
            );
            changes.setIngredientStocks(slice.liveRows().stream().map(this::toIngredientStockDto).toList());
            deletions.setIngredientStocks(slice.deletedRows().stream().map(IngredientStockSyncRow::sku).toList());
            nextCursors.setIngredientStocks(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.RECIPES)) {
            SliceResult<MenuRecipeSyncRow> slice = buildSlice(
                    posSyncRepository.findRecipeRows(
                            restaurantId,
                            cursorTime(nextCursors.getRecipes()),
                            parseLongCursor(nextCursors.getRecipes(), SyncGroup.RECIPES),
                            firstPage(pageSize)
                    ),
                    nextCursors.getRecipes(),
                    pageSize
            );
            changes.setRecipes(slice.rows().stream().map(this::toRecipeDto).toList());
            nextCursors.setRecipes(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.RECIPE_ITEMS)) {
            SliceResult<MenuRecipeItemSyncRow> slice = buildSlice(
                    posSyncRepository.findRecipeItemRows(
                            restaurantId,
                            cursorTime(nextCursors.getRecipeItems()),
                            parseLongCursor(nextCursors.getRecipeItems(), SyncGroup.RECIPE_ITEMS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getRecipeItems(),
                    pageSize
            );
            changes.setRecipeItems(slice.rows().stream().map(this::toRecipeItemDto).toList());
            nextCursors.setRecipeItems(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.PREPARED_STOCKS)) {
            SliceResult<PreparedStockSyncRow> slice = buildSlice(
                    posSyncRepository.findPreparedStockRows(
                            restaurantId,
                            cursorTime(nextCursors.getPreparedStocks()),
                            parseLongCursor(nextCursors.getPreparedStocks(), SyncGroup.PREPARED_STOCKS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getPreparedStocks(),
                    pageSize
            );
            changes.setPreparedStocks(slice.rows().stream().map(this::toPreparedStockDto).toList());
            nextCursors.setPreparedStocks(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        if (groups.contains(SyncGroup.SETTINGS)) {
            SliceResult<PosSettingSyncRow> slice = buildSlice(
                    posSyncRepository.findSettingRows(
                            restaurantId,
                            cursorTime(nextCursors.getSettings()),
                            parseLongCursor(nextCursors.getSettings(), SyncGroup.SETTINGS),
                            firstPage(pageSize)
                    ),
                    nextCursors.getSettings(),
                    pageSize
            );
            changes.setSettings(slice.rows().stream().map(this::toSettingDto).toList());
            nextCursors.setSettings(slice.nextCursor());
            hasMore = hasMore || slice.hasMore();
        }

        return PosSyncResponse.builder()
                .serverTime(Instant.now())
                .syncSessionId(buildSyncSessionId(deviceId))
                .changes(changes)
                .deletions(deletions)
                .nextCursors(nextCursors)
                .hasMore(hasMore)
                .build();
    }

    private Set<SyncGroup> resolveRequestedGroups(List<String> requestedGroups) {
        Set<SyncGroup> groups = new LinkedHashSet<>();
        if (requestedGroups == null || requestedGroups.isEmpty()) {
            groups.addAll(List.of(SyncGroup.values()));
            return groups;
        }

        for (String requestedGroup : requestedGroups) {
            SyncGroup group = SyncGroup.fromValue(requestedGroup);
            if (group == null) {
                throw new BadRequestException("Unsupported requested group: " + requestedGroup);
            }
            groups.add(group);
        }
        return groups;
    }

    private int resolvePageSize(Integer requestedPageSize) {
        int resolved = requestedPageSize == null ? DEFAULT_PAGE_SIZE : requestedPageSize;
        if (resolved < 1) {
            throw new BadRequestException("pageSize must be at least 1");
        }
        return Math.min(resolved, MAX_PAGE_SIZE);
    }

    private PageRequest firstPage(int pageSize) {
        return PageRequest.of(0, pageSize + 1);
    }

    private LocalDateTime cursorTime(TimeCursorDto cursor) {
        return LocalDateTime.ofInstant(normalizeCursor(cursor).getLastSyncTime(), CURSOR_ZONE);
    }

    private Long parseLongCursor(TimeCursorDto cursor, SyncGroup group) {
        String lastSeenKey = normalizeCursor(cursor).getLastSeenKey();
        if (!StringUtils.hasText(lastSeenKey)) {
            return 0L;
        }
        try {
            return Long.parseLong(lastSeenKey);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("Invalid cursor key for group " + group.groupName());
        }
    }

    private String parseStringCursor(TimeCursorDto cursor) {
        String lastSeenKey = normalizeCursor(cursor).getLastSeenKey();
        return lastSeenKey == null ? "" : lastSeenKey;
    }

    private TimeCursorDto normalizeCursor(TimeCursorDto cursor) {
        return cursor == null
                ? new TimeCursorDto(Instant.EPOCH, null)
                : new TimeCursorDto(
                        cursor.getLastSyncTime() == null ? Instant.EPOCH : cursor.getLastSyncTime(),
                        StringUtils.hasText(cursor.getLastSeenKey()) ? cursor.getLastSeenKey() : null
                );
    }

    private SyncTimeCursorBundle copyCursorBundle(SyncTimeCursorBundle incoming) {
        SyncTimeCursorBundle bundle = new SyncTimeCursorBundle();
        if (incoming == null) {
            bundle.setCategories(normalizeCursor(null));
            bundle.setMenuItems(normalizeCursor(null));
            bundle.setPrices(normalizeCursor(null));
            bundle.setTaxes(normalizeCursor(null));
            bundle.setTaxClasses(normalizeCursor(null));
            bundle.setTaxDefinitions(normalizeCursor(null));
            bundle.setTaxRules(normalizeCursor(null));
            bundle.setTaxRegistrations(normalizeCursor(null));
            bundle.setItemStocks(normalizeCursor(null));
            bundle.setIngredientStocks(normalizeCursor(null));
            bundle.setRecipes(normalizeCursor(null));
            bundle.setRecipeItems(normalizeCursor(null));
            bundle.setPreparedStocks(normalizeCursor(null));
            bundle.setSettings(normalizeCursor(null));
            return bundle;
        }

        bundle.setCategories(normalizeCursor(incoming.getCategories()));
        bundle.setMenuItems(normalizeCursor(incoming.getMenuItems()));
        bundle.setPrices(normalizeCursor(incoming.getPrices()));
        bundle.setTaxes(normalizeCursor(incoming.getTaxes()));
        bundle.setTaxClasses(normalizeCursor(incoming.getTaxClasses()));
        bundle.setTaxDefinitions(normalizeCursor(incoming.getTaxDefinitions()));
        bundle.setTaxRules(normalizeCursor(incoming.getTaxRules()));
        bundle.setTaxRegistrations(normalizeCursor(incoming.getTaxRegistrations()));
        bundle.setItemStocks(normalizeCursor(incoming.getItemStocks()));
        bundle.setIngredientStocks(normalizeCursor(incoming.getIngredientStocks()));
        bundle.setRecipes(normalizeCursor(incoming.getRecipes()));
        bundle.setRecipeItems(normalizeCursor(incoming.getRecipeItems()));
        bundle.setPreparedStocks(normalizeCursor(incoming.getPreparedStocks()));
        bundle.setSettings(normalizeCursor(incoming.getSettings()));
        return bundle;
    }

    private <T extends SyncStreamRow> SliceResult<T> buildSlice(List<T> rows, TimeCursorDto incomingCursor, int pageSize) {
        boolean hasMore = rows.size() > pageSize;
        List<T> includedRows = new ArrayList<>(rows.subList(0, Math.min(rows.size(), pageSize)));
        TimeCursorDto nextCursor = includedRows.isEmpty()
                ? normalizeCursor(incomingCursor)
                : toCursor(includedRows.get(includedRows.size() - 1));
        return new SliceResult<>(includedRows, nextCursor, hasMore);
    }

    private <T extends TombstoneSyncRow> TombstoneSlice<T> buildTombstoneSlice(List<T> rows,
                                                                               TimeCursorDto incomingCursor,
                                                                               int pageSize) {
        SliceResult<T> slice = buildSlice(rows, incomingCursor, pageSize);
        List<T> liveRows = slice.rows().stream().filter(row -> !Boolean.TRUE.equals(row.isDeleted())).toList();
        List<T> deletedRows = slice.rows().stream().filter(row -> Boolean.TRUE.equals(row.isDeleted())).toList();
        return new TombstoneSlice<>(liveRows, deletedRows, slice.nextCursor(), slice.hasMore());
    }

    private TimeCursorDto toCursor(SyncStreamRow row) {
        return new TimeCursorDto(row.syncTs().toInstant(CURSOR_ZONE), row.cursorKey());
    }

    private String buildSyncSessionId(String deviceId) {
        String sessionId = UUID.randomUUID().toString();
        return StringUtils.hasText(deviceId) ? deviceId.trim() + ":" + sessionId : sessionId;
    }

    private CategorySyncDto toCategoryDto(CategorySyncRow row) {
        return new CategorySyncDto(
                row.categoryId(),
                row.restaurantId(),
                row.categoryName(),
                row.categoryDescription(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private MenuItemSyncDto toMenuItemDto(MenuItemSyncRow row) {
        return new MenuItemSyncDto(
                row.menuItemId(),
                row.restaurantId(),
                row.categoryId(),
                row.priceId(),
                row.taxClassId(),
                ProductImageUrlUtil.toClientUrl(row.productImageUrl()),
                row.itemName(),
                row.description(),
                row.isAvailable(),
                row.isActive(),
                row.isTrending(),
                row.menuType(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private MenuPriceSyncDto toPriceDto(MenuPriceSyncRow row) {
        return new MenuPriceSyncDto(
                row.priceId(),
                row.menuItemId(),
                row.restaurantId(),
                row.price(),
                row.discount(),
                row.priceIncludesTax(),
                row.syncUpdatedAt()
        );
    }

    private TaxConfigSyncDto toTaxDto(TaxConfigSyncRow row) {
        return new TaxConfigSyncDto(
                row.taxId(),
                row.restaurantId(),
                row.taxName(),
                row.taxAmount(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private TaxClassSyncDto toTaxClassDto(TaxClassSyncRow row) {
        return new TaxClassSyncDto(
                row.id(),
                row.restaurantId(),
                row.code(),
                row.name(),
                row.description(),
                row.isExempt(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private TaxDefinitionSyncDto toTaxDefinitionDto(TaxDefinitionSyncRow row) {
        return new TaxDefinitionSyncDto(
                row.id(),
                row.restaurantId(),
                row.code(),
                row.displayName(),
                row.kind(),
                row.valueType(),
                row.defaultValue(),
                row.currencyCode(),
                row.isRecoverable(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private TaxRuleSyncDto toTaxRuleDto(TaxRuleSyncRow row) {
        return new TaxRuleSyncDto(
                row.id(),
                row.restaurantId(),
                row.taxDefinitionId(),
                row.taxClassId(),
                row.calculationMode(),
                row.compoundMode(),
                row.sequenceNo(),
                row.validFrom(),
                row.validTo(),
                row.countryCode(),
                row.regionCode(),
                row.buyerTaxCategory(),
                row.minAmount(),
                row.maxAmount(),
                row.priority(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private TaxRegistrationSyncDto toTaxRegistrationDto(TaxRegistrationSyncRow row) {
        return new TaxRegistrationSyncDto(
                row.id(),
                row.restaurantId(),
                row.schemeCode(),
                row.registrationNumber(),
                row.legalName(),
                row.countryCode(),
                row.regionCode(),
                row.placeOfBusiness(),
                row.isDefault(),
                row.validFrom(),
                row.validTo(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private ItemStockSyncDto toItemStockDto(ItemStockSyncRow row) {
        return new ItemStockSyncDto(
                row.sku(),
                row.restaurantId(),
                row.menuItemId(),
                row.supplierId(),
                row.supplierName(),
                row.totalStock(),
                row.reorderLevel(),
                row.unitOfMeasure(),
                row.isActive(),
                row.lastRestockedAt(),
                row.updatedAt()
        );
    }

    private IngredientStockSyncDto toIngredientStockDto(IngredientStockSyncRow row) {
        return new IngredientStockSyncDto(
                row.sku(),
                row.restaurantId(),
                row.ingredientName(),
                row.description(),
                row.supplierId(),
                row.supplierName(),
                row.totalStock(),
                row.reorderLevel(),
                row.unitOfMeasure(),
                row.isActive(),
                row.lastRestockedAt(),
                row.updatedAt()
        );
    }

    private MenuRecipeSyncDto toRecipeDto(MenuRecipeSyncRow row) {
        return new MenuRecipeSyncDto(
                row.recipeId(),
                row.menuItemId(),
                row.restaurantId(),
                row.batchSize(),
                row.isActive(),
                row.syncUpdatedAt()
        );
    }

    private MenuRecipeItemSyncDto toRecipeItemDto(MenuRecipeItemSyncRow row) {
        return new MenuRecipeItemSyncDto(
                row.recipeItemId(),
                row.recipeId(),
                row.menuItemId(),
                row.restaurantId(),
                row.ingredientSku(),
                row.quantityRequired(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private PreparedStockSyncDto toPreparedStockDto(PreparedStockSyncRow row) {
        return new PreparedStockSyncDto(
                row.menuItemId(),
                row.restaurantId(),
                row.availableQty(),
                row.reservedQty(),
                row.unitCode(),
                row.isActive(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private PosSettingSyncDto toSettingDto(PosSettingSyncRow row) {
        return new PosSettingSyncDto(
                row.restaurantId(),
                row.chainId(),
                row.restaurantCode(),
                row.restaurantName(),
                row.currency(),
                row.timezone(),
                row.gstNumber(),
                row.phoneNumber(),
                row.email(),
                row.updatedAt()
        );
    }

    private record SliceResult<T extends SyncStreamRow>(List<T> rows, TimeCursorDto nextCursor, boolean hasMore) {
    }

    private record TombstoneSlice<T extends TombstoneSyncRow>(List<T> liveRows,
                                                              List<T> deletedRows,
                                                              TimeCursorDto nextCursor,
                                                              boolean hasMore) {
    }
}
