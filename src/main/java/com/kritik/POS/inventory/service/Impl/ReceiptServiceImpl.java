package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.entity.stockEntry.StockReceipt;
import com.kritik.POS.inventory.entity.stockEntry.StockReceiptItem;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;
import com.kritik.POS.inventory.models.request.StockReceiptCreateRequest;
import com.kritik.POS.inventory.models.response.ItemUnitConversionResponse;
import com.kritik.POS.inventory.models.response.StockReceiptResponse;
import com.kritik.POS.inventory.models.response.StockReceiptSkuOptionDto;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import com.kritik.POS.inventory.models.response.UnitSummaryResponse;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.repository.StockReceiptRepository;
import com.kritik.POS.inventory.service.ItemUnitConversionService;
import com.kritik.POS.inventory.service.ReceiptService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {
    private final StockReceiptRepository stockReceiptRepository;
    private final StockRepository stockRepository;
    private final IngredientStockRepository ingredientStockRepository;
    private final TenantAccessService tenantAccessService;
    private final InventoryUtil inventoryUtil;
    private final ItemUnitConversionService itemUnitConversionService;

    @Override
    public PageResponse<StockReceiptResponseDto> getReceiptPage(Long chainId, Long restaurantId, String search, Integer pageNumber, Integer pageSize) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<StockReceiptResponseDto> page = stockReceiptRepository.findReceiptSummaries(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        InventoryUtil.normalizeSearch(search),
                        PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "receivedAt"))
                )
                .map(StockReceiptResponseDto::toStockReceiptDto);
        return PageResponse.from(page);
    }

    @Override
    public StockReceiptResponse getReceiptById(Long receiptId) {
        StockReceipt stockReceipt = stockReceiptRepository.findByReceiptIdAndIsDeletedFalse(receiptId)
                .orElseThrow(() -> new AppException("Stock receipt not found", HttpStatus.BAD_REQUEST));
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(stockReceipt.getRestaurantId());
        }
        return StockReceiptResponse.fromEntity(stockReceipt);
    }

    @Override
    public List<StockReceiptSkuOptionDto> getReceiptSkuOptions(Long supplierId) {
        List<Long> accessibleRestaurantIds;
        if (supplierId != null) {
            Supplier supplier = inventoryUtil.getAccessibleSupplier(supplierId, null);
            accessibleRestaurantIds = List.of(supplier.getRestaurantId());
        } else {
            accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
            if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
                return List.of();
            }
        }

        List<StockReceiptSkuOptionDto> ingredientOptions = ingredientStockRepository.findReceiptIngredients(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        supplierId
                ).stream()
                .map(this::toIngredientReceiptOption)
                .toList();

        List<StockReceiptSkuOptionDto> directOptions = stockRepository.findReceiptStocks(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        supplierId
                ).stream()
                .map(this::toDirectReceiptOption)
                .toList();

        return Stream.concat(ingredientOptions.stream(), directOptions.stream())
                .sorted(Comparator.comparing(StockReceiptSkuOptionDto::skuName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(option -> option.skuType().name())
                        .thenComparing(StockReceiptSkuOptionDto::sku))
                .toList();
    }

    @Transactional
    @Override
    public StockReceiptResponse createStockReceipt(StockReceiptCreateRequest stockReceiptCreateRequest) {
        Supplier supplier = inventoryUtil.getAccessibleSupplier(stockReceiptCreateRequest.supplierId(), stockReceiptCreateRequest.restaurantId());
        Long restaurantId = supplier.getRestaurantId();
        if (stockReceiptCreateRequest.restaurantId() != null && !stockReceiptCreateRequest.restaurantId().equals(restaurantId)) {
            throw new AppException("Supplier does not belong to the requested restaurant", HttpStatus.BAD_REQUEST);
        }

        StockReceipt stockReceipt = new StockReceipt();
        stockReceipt.setReceiptNumber(generateReceiptNumber());
        stockReceipt.setRestaurantId(restaurantId);
        stockReceipt.setSupplier(supplier);
        stockReceipt.setInvoiceNumber(InventoryUtil.trimToNull(stockReceiptCreateRequest.invoiceNumber()));
        stockReceipt.setReceivedAt(stockReceiptCreateRequest.receivedAt() == null ? LocalDateTime.now() : stockReceiptCreateRequest.receivedAt());
        stockReceipt.setNotes(InventoryUtil.trimToNull(stockReceiptCreateRequest.notes()));

        List<StockReceiptItem> receiptItems = new ArrayList<>();
        double totalQuantity = 0.0;
        double totalCost = 0.0;

        for (StockReceiptCreateRequest.ReceiptItemRequest itemRequest : stockReceiptCreateRequest.items()) {
            StockReceiptItem receiptItem = new StockReceiptItem();
            receiptItem.setStockReceipt(stockReceipt);
            receiptItem.setSkuType(itemRequest.skuType());
            BigDecimal enteredQty = resolveEnteredQty(itemRequest);
            receiptItem.setEnteredQty(enteredQty.doubleValue());
            receiptItem.setUnitCost(itemRequest.unitCost());
            receiptItem.setTotalCost(itemRequest.unitCost() * enteredQty.doubleValue());

            if (itemRequest.skuType() == StockReceiptSkuType.INGREDIENT) {
                BigDecimal baseQty = applyIngredientReceiptItem(receiptItem, itemRequest, supplier, restaurantId, stockReceipt.getReceivedAt(), enteredQty);
                totalQuantity += baseQty.doubleValue();
            } else if (itemRequest.skuType() == StockReceiptSkuType.DIRECT_MENU) {
                BigDecimal baseQty = applyDirectReceiptItem(receiptItem, itemRequest, supplier, restaurantId, stockReceipt.getReceivedAt(), enteredQty);
                totalQuantity += baseQty.doubleValue();
            } else {
                throw new AppException("Unsupported receipt SKU type", HttpStatus.BAD_REQUEST);
            }

            receiptItems.add(receiptItem);
            totalCost += receiptItem.getTotalCost();
        }

        stockReceipt.setReceiptItems(receiptItems);
        stockReceipt.setTotalItems(receiptItems.size());
        stockReceipt.setTotalQuantity(totalQuantity);
        stockReceipt.setTotalCost(totalCost);

        return StockReceiptResponse.fromEntity(stockReceiptRepository.save(stockReceipt));
    }

    private BigDecimal applyIngredientReceiptItem(StockReceiptItem receiptItem,
                                                  StockReceiptCreateRequest.ReceiptItemRequest itemRequest,
                                                  Supplier supplier,
                                                  Long restaurantId,
                                                  LocalDateTime receivedAt,
                                                  BigDecimal enteredQty) {
        IngredientStock ingredientStock = inventoryUtil.getAccessibleIngredient(itemRequest.sku());
        if (!restaurantId.equals(ingredientStock.getRestaurantId())) {
            throw new AppException("All receipt items must belong to the same restaurant as the supplier", HttpStatus.BAD_REQUEST);
        }
        BigDecimal baseQuantity = itemUnitConversionService.convertToBase(
                restaurantId,
                UnitConversionSourceType.INGREDIENT,
                ingredientStock.getSku(),
                resolveUnitId(itemRequest, ingredientStock.getBaseUnit() == null ? null : ingredientStock.getBaseUnit().getId()),
                enteredQty
        );

        ingredientStock.setSupplier(supplier);
        ingredientStock.setIsActive(true);
        ingredientStock.setTotalStock(ingredientStock.getTotalStock() + baseQuantity.doubleValue());
        ingredientStock.setLastRestockedAt(receivedAt);
        inventoryUtil.syncMenuAvailabilityForIngredient(ingredientStock.getSku());

        receiptItem.setIngredientStock(ingredientStock);
        receiptItem.setSkuName(ingredientStock.getIngredientName());
        receiptItem.setQuantityReceived(baseQuantity.doubleValue());
        receiptItem.setUnit(resolveReceiptUnit(restaurantId, UnitConversionSourceType.INGREDIENT, ingredientStock.getSku(), itemRequest.unitId(), ingredientStock.getBaseUnit()));
        return baseQuantity;
    }

    private BigDecimal applyDirectReceiptItem(StockReceiptItem receiptItem,
                                              StockReceiptCreateRequest.ReceiptItemRequest itemRequest,
                                              Supplier supplier,
                                              Long restaurantId,
                                              LocalDateTime receivedAt,
                                              BigDecimal enteredQty) {
        ItemStock itemStock = inventoryUtil.getAccessibleStock(itemRequest.sku());
        if (!restaurantId.equals(itemStock.getRestaurantId())) {
            throw new AppException("All receipt items must belong to the same restaurant as the supplier", HttpStatus.BAD_REQUEST);
        }
        BigDecimal baseQuantity = itemUnitConversionService.convertToBase(
                restaurantId,
                UnitConversionSourceType.DIRECT_ITEM,
                String.valueOf(itemStock.getMenuItem().getId()),
                resolveUnitId(itemRequest, itemStock.getMenuItem().getBaseUnit() == null ? null : itemStock.getMenuItem().getBaseUnit().getId()),
                enteredQty
        );
        if (baseQuantity.stripTrailingZeros().scale() > 0) {
            throw new AppException("Direct item conversions must resolve to whole base units", HttpStatus.BAD_REQUEST);
        }

        itemStock.setSupplier(supplier);
        itemStock.setIsActive(true);
        itemStock.setTotalStock(itemStock.getTotalStock() + baseQuantity.intValueExact());
        itemStock.setLastRestockedAt(receivedAt);
        InventoryUtil.syncMenuAvailability(itemStock);

        receiptItem.setItemStock(itemStock);
        receiptItem.setSkuName(itemStock.getMenuItem().getItemName());
        receiptItem.setQuantityReceived(baseQuantity.doubleValue());
        receiptItem.setUnit(resolveReceiptUnit(
                restaurantId,
                UnitConversionSourceType.DIRECT_ITEM,
                String.valueOf(itemStock.getMenuItem().getId()),
                itemRequest.unitId(),
                itemStock.getMenuItem().getBaseUnit()
        ));
        return baseQuantity;
    }

    private String generateReceiptNumber() {
        String receiptNumber;
        do {
            receiptNumber = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (stockReceiptRepository.existsByReceiptNumber(receiptNumber));
        return receiptNumber;
    }

    private StockReceiptSkuOptionDto toIngredientReceiptOption(IngredientStock ingredientStock) {
        List<ItemUnitConversionResponse> purchaseUnits = itemUnitConversionService.getConversions(
                        ingredientStock.getRestaurantId(),
                        UnitConversionSourceType.INGREDIENT,
                        ingredientStock.getSku())
                .stream()
                .filter(conversion -> Boolean.TRUE.equals(conversion.getActive()) && Boolean.TRUE.equals(conversion.getPurchaseAllowed()))
                .map(ItemUnitConversionResponse::fromEntity)
                .toList();
        return new StockReceiptSkuOptionDto(
                ingredientStock.getSku(),
                ingredientStock.getIngredientName(),
                StockReceiptSkuType.INGREDIENT,
                ingredientStock.getUnitOfMeasure(),
                ingredientStock.getTotalStock(),
                UnitSummaryResponse.fromEntity(ingredientStock.getBaseUnit()),
                purchaseUnits
        );
    }

    private StockReceiptSkuOptionDto toDirectReceiptOption(ItemStock itemStock) {
        List<ItemUnitConversionResponse> purchaseUnits = itemUnitConversionService.getConversions(
                        itemStock.getRestaurantId(),
                        UnitConversionSourceType.DIRECT_ITEM,
                        String.valueOf(itemStock.getMenuItem().getId()))
                .stream()
                .filter(conversion -> Boolean.TRUE.equals(conversion.getActive()) && Boolean.TRUE.equals(conversion.getPurchaseAllowed()))
                .map(ItemUnitConversionResponse::fromEntity)
                .toList();
        String unitCode = itemStock.getMenuItem().getBaseUnit() == null
                ? itemStock.getUnitOfMeasure()
                : itemStock.getMenuItem().getBaseUnit().getCode();
        return new StockReceiptSkuOptionDto(
                itemStock.getSku(),
                itemStock.getMenuItem().getItemName(),
                StockReceiptSkuType.DIRECT_MENU,
                unitCode,
                itemStock.getTotalStock() == null ? 0.0 : itemStock.getTotalStock().doubleValue(),
                UnitSummaryResponse.fromEntity(itemStock.getMenuItem().getBaseUnit()),
                purchaseUnits
        );
    }

    private BigDecimal resolveEnteredQty(StockReceiptCreateRequest.ReceiptItemRequest itemRequest) {
        if (itemRequest.enteredQty() != null && itemRequest.enteredQty() > 0) {
            return BigDecimal.valueOf(itemRequest.enteredQty());
        }
        if (itemRequest.quantityReceived() != null && itemRequest.quantityReceived() > 0) {
            return BigDecimal.valueOf(itemRequest.quantityReceived());
        }
        throw new AppException("Entered quantity is required", HttpStatus.BAD_REQUEST);
    }

    private Long resolveUnitId(StockReceiptCreateRequest.ReceiptItemRequest itemRequest, Long defaultUnitId) {
        return itemRequest.unitId() != null ? itemRequest.unitId() : defaultUnitId;
    }

    private com.kritik.POS.inventory.entity.unit.UnitMaster resolveReceiptUnit(Long restaurantId,
                                                                               UnitConversionSourceType sourceType,
                                                                               String sourceId,
                                                                               Long requestedUnitId,
                                                                               com.kritik.POS.inventory.entity.unit.UnitMaster defaultUnit) {
        Long unitId = requestedUnitId != null ? requestedUnitId : defaultUnit == null ? null : defaultUnit.getId();
        if (unitId == null) {
            return defaultUnit;
        }
        return itemUnitConversionService.getConversions(restaurantId, sourceType, sourceId).stream()
                .map(ItemUnitConversion::getUnit)
                .filter(unit -> unitId.equals(unit.getId()))
                .findFirst()
                .orElse(defaultUnit);
    }


}
