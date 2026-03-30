package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.enums.StockReceiptSkuType;
import com.kritik.POS.inventory.models.response.StockReceiptSkuOptionDto;
import com.kritik.POS.inventory.entity.*;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.StockRepository;
import com.kritik.POS.inventory.repository.StockReceiptRepository;
import com.kritik.POS.inventory.service.ReceiptService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.restaurant.models.request.StockReceiptCreateRequest;
import com.kritik.POS.restaurant.models.response.StockReceiptResponse;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

        List<StockReceiptSkuOptionDto> ingredientOptions = ingredientStockRepository.findReceiptSkuOptions(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        supplierId
                ).stream()
                .map(StockReceiptSkuOptionDto::fromProjection)
                .toList();

        List<StockReceiptSkuOptionDto> directOptions = stockRepository.findReceiptSkuOptions(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        supplierId
                ).stream()
                .map(StockReceiptSkuOptionDto::fromProjection)
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
        int totalQuantity = 0;
        double totalCost = 0.0;

        for (StockReceiptCreateRequest.ReceiptItemRequest itemRequest : stockReceiptCreateRequest.items()) {
            StockReceiptItem receiptItem = new StockReceiptItem();
            receiptItem.setStockReceipt(stockReceipt);
            receiptItem.setSkuType(itemRequest.skuType());
            receiptItem.setQuantityReceived(itemRequest.quantityReceived());
            receiptItem.setUnitCost(itemRequest.unitCost());
            receiptItem.setTotalCost(itemRequest.unitCost() * itemRequest.quantityReceived());

            if (itemRequest.skuType() == StockReceiptSkuType.INGREDIENT) {
                applyIngredientReceiptItem(receiptItem, itemRequest, supplier, restaurantId, stockReceipt.getReceivedAt());
            } else if (itemRequest.skuType() == StockReceiptSkuType.DIRECT_MENU) {
                applyDirectReceiptItem(receiptItem, itemRequest, supplier, restaurantId, stockReceipt.getReceivedAt());
            } else {
                throw new AppException("Unsupported receipt SKU type", HttpStatus.BAD_REQUEST);
            }

            receiptItems.add(receiptItem);
            totalQuantity += itemRequest.quantityReceived();
            totalCost += receiptItem.getTotalCost();
        }

        stockReceipt.setReceiptItems(receiptItems);
        stockReceipt.setTotalItems(receiptItems.size());
        stockReceipt.setTotalQuantity(totalQuantity);
        stockReceipt.setTotalCost(totalCost);

        return StockReceiptResponse.fromEntity(stockReceiptRepository.save(stockReceipt));
    }

    private void applyIngredientReceiptItem(StockReceiptItem receiptItem,
                                            StockReceiptCreateRequest.ReceiptItemRequest itemRequest,
                                            Supplier supplier,
                                            Long restaurantId,
                                            LocalDateTime receivedAt) {
        IngredientStock ingredientStock = inventoryUtil.getAccessibleIngredient(itemRequest.sku());
        if (!restaurantId.equals(ingredientStock.getRestaurantId())) {
            throw new AppException("All receipt items must belong to the same restaurant as the supplier", HttpStatus.BAD_REQUEST);
        }

        ingredientStock.setSupplier(supplier);
        ingredientStock.setIsActive(true);
        ingredientStock.setTotalStock(ingredientStock.getTotalStock() + itemRequest.quantityReceived());
        ingredientStock.setLastRestockedAt(receivedAt);
        inventoryUtil.syncMenuAvailabilityForIngredient(ingredientStock.getSku());

        receiptItem.setIngredientStock(ingredientStock);
        receiptItem.setSkuName(ingredientStock.getIngredientName());
    }

    private void applyDirectReceiptItem(StockReceiptItem receiptItem,
                                        StockReceiptCreateRequest.ReceiptItemRequest itemRequest,
                                        Supplier supplier,
                                        Long restaurantId,
                                        LocalDateTime receivedAt) {
        ItemStock itemStock = inventoryUtil.getAccessibleStock(itemRequest.sku());
        if (!restaurantId.equals(itemStock.getRestaurantId())) {
            throw new AppException("All receipt items must belong to the same restaurant as the supplier", HttpStatus.BAD_REQUEST);
        }

        itemStock.setSupplier(supplier);
        itemStock.setIsActive(true);
        itemStock.setTotalStock(itemStock.getTotalStock() + itemRequest.quantityReceived());
        itemStock.setLastRestockedAt(receivedAt);
        InventoryUtil.syncMenuAvailability(itemStock);

        receiptItem.setItemStock(itemStock);
        receiptItem.setSkuName(itemStock.getMenuItem().getItemName());
    }

    private String generateReceiptNumber() {
        String receiptNumber;
        do {
            receiptNumber = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        } while (stockReceiptRepository.existsByReceiptNumber(receiptNumber));
        return receiptNumber;
    }


}
