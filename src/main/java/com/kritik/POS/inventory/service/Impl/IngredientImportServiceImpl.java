package com.kritik.POS.inventory.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.stockEntry.Supplier;
import com.kritik.POS.inventory.models.response.IngredientImportCommitResponse;
import com.kritik.POS.inventory.models.response.IngredientImportPreviewResponse;
import com.kritik.POS.inventory.models.response.IngredientImportRowResult;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.SupplierRepository;
import com.kritik.POS.inventory.service.IngredientImportService;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngredientImportServiceImpl implements IngredientImportService {

    private static final long MAX_FILE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final int MAX_ROWS = 5_000;
    private static final int PREVIEW_TTL_MINUTES = 10;
    private static final int BATCH_SIZE = 200;
    private static final Set<String> ALLOWED_HEADERS = Set.of(
            "sku", "ingredient_name", "description", "restaurant_id", "total_stock",
            "unit_code", "category", "reorder_level", "supplier_id", "supplier_name", "is_active"
    );

    private final IngredientStockRepository ingredientStockRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryService inventoryService;
    private final TenantAccessService tenantAccessService;
    private final IngredientImportPreviewStore previewStore;

    @Override
    public IngredientImportPreviewResponse previewImport(MultipartFile file, Long restaurantId, Boolean overwriteNulls) {
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(restaurantId);
        byte[] csvBytes = readValidatedBytes(file);
        ParsedCsvPreview preview = parsePreview(csvBytes, manageableRestaurantId);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PREVIEW_TTL_MINUTES);
        IngredientImportPreviewStore.PreviewSession session = new IngredientImportPreviewStore.PreviewSession(
                token,
                currentUserId(),
                manageableRestaurantId,
                Boolean.TRUE.equals(overwriteNulls),
                sha256(csvBytes),
                expiresAt,
                preview.normalizedHeaders(),
                preview.validRows(),
                preview.rowResults(),
                preview.invalidRowCount()
        );
        previewStore.save(session);

        log.info(
                "Ingredient import preview created: userId={}, restaurantId={}, totalRows={}, validRows={}, invalidRows={}, creates={}, updates={}",
                session.userId(),
                manageableRestaurantId,
                preview.totalRowsRead(),
                preview.validRows().size(),
                preview.invalidRowCount(),
                preview.rowsToCreate(),
                preview.rowsToUpdate()
        );

        return new IngredientImportPreviewResponse(
                token,
                expiresAt,
                preview.totalRowsRead(),
                preview.validRows().size(),
                preview.invalidRowCount(),
                preview.rowsToCreate(),
                preview.rowsToUpdate(),
                preview.normalizedHeaders(),
                preview.rowResults()
        );
    }

    @Transactional
    @Override
    public IngredientImportCommitResponse commitImport(String previewToken) {
        IngredientImportPreviewStore.PreviewSession session = requirePreviewSession(previewToken);
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(session.restaurantId());
        if (session.validRows().isEmpty()) {
            throw new AppException("Preview contains no valid rows to import", HttpStatus.BAD_REQUEST);
        }

        long startedAt = System.currentTimeMillis();
        Map<String, IngredientStock> existingBySku = ingredientStockRepository.findAllBySkuIn(
                        session.validRows().stream().map(IngredientImportPreviewStore.ParsedIngredientRow::getSku).toList()
                ).stream()
                .collect(Collectors.toMap(IngredientStock::getSku, ingredient -> ingredient, (left, right) -> left));
        SupplierLookup supplierLookup = buildSupplierLookup(manageableRestaurantId);

        int createdCount = 0;
        int updatedCount = 0;
        Set<String> changedSkus = new LinkedHashSet<>();
        List<IngredientStock> batch = new ArrayList<>();

        for (IngredientImportPreviewStore.ParsedIngredientRow row : session.validRows()) {
            IngredientStock ingredient = existingBySku.get(row.getSku());
            boolean creating = ingredient == null;
            if (creating) {
                ingredient = new IngredientStock();
                ingredient.setSku(row.getSku());
                createdCount++;
            } else {
                updatedCount++;
                if (!Objects.equals(ingredient.getRestaurantId(), manageableRestaurantId)) {
                    throw new AppException("Ingredient " + row.getSku() + " does not belong to the selected restaurant", HttpStatus.BAD_REQUEST);
                }
            }

            Double previousStock = ingredient.getTotalStock();
            ingredient.setRestaurantId(manageableRestaurantId);
            ingredient.setIngredientName(row.getIngredientName());
            ingredient.setTotalStock(creating
                    ? (row.getTotalStock() == null ? 0.0 : row.getTotalStock())
                    : (ingredient.getTotalStock() == null ? 0.0 : ingredient.getTotalStock()));
            ingredient.setIsDeleted(false);
            ingredient.setDescription(resolveNullableString("description", row.getDescription(), ingredient.getDescription(), session));
            ingredient.setCategory(resolveNullableString("category", row.getCategory(), ingredient.getCategory(), session));
            ingredient.setReorderLevel(resolveDoubleValue("reorder_level", row.getReorderLevel(), ingredient.getReorderLevel(), 0.0, session));
            ingredient.setUnitOfMeasure(resolveStringValue("unit_code", row.getUnitCode(), ingredient.getUnitOfMeasure(), "unit", session));
            ingredient.setIsActive(resolveBooleanValue("is_active", row.getIsActive(), ingredient.getIsActive(), true, session));
            ingredient.setSupplier(resolveSupplier(row, supplierLookup, ingredient.getSupplier(), session));
            double previousStockValue = previousStock == null ? 0.0 : previousStock;
            double currentStockValue = ingredient.getTotalStock() == null ? 0.0 : ingredient.getTotalStock();
            if (currentStockValue > previousStockValue) {
                ingredient.setLastRestockedAt(LocalDateTime.now());
            }

            batch.add(ingredient);
            changedSkus.add(row.getSku());
            if (batch.size() >= BATCH_SIZE) {
                ingredientStockRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            ingredientStockRepository.saveAll(batch);
        }

        boolean refreshedAvailability = !changedSkus.isEmpty();
        if (refreshedAvailability) {
            inventoryService.refreshMenuAvailability(List.of(), changedSkus);
        }

        previewStore.remove(previewToken);
        long elapsed = System.currentTimeMillis() - startedAt;
        log.info(
                "Ingredient import committed: userId={}, restaurantId={}, created={}, updated={}, invalidRows={}, refreshedAvailability={}, elapsedMs={}",
                currentUserId(),
                manageableRestaurantId,
                createdCount,
                updatedCount,
                session.invalidRowCount(),
                refreshedAvailability,
                elapsed
        );

        return new IngredientImportCommitResponse(
                createdCount,
                updatedCount,
                session.invalidRowCount(),
                elapsed,
                refreshedAvailability,
                session.rowResults()
        );
    }

    private IngredientImportPreviewStore.PreviewSession requirePreviewSession(String previewToken) {
        IngredientImportPreviewStore.PreviewSession session = previewStore.get(previewToken);
        if (session == null) {
            throw new AppException("Preview token is invalid or expired", HttpStatus.BAD_REQUEST);
        }
        if (!Objects.equals(session.userId(), currentUserId())) {
            throw new AppException("Preview token is invalid for the current user", HttpStatus.FORBIDDEN);
        }
        return session;
    }

    private ParsedCsvPreview parsePreview(byte[] csvBytes, Long restaurantId) {
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(new ByteArrayInputStream(csvBytes), utf8Decoder()))) {
            String[] rawHeader = csvReader.readNext();
            if (rawHeader == null) {
                throw new AppException("CSV file is empty", HttpStatus.BAD_REQUEST);
            }

            List<String> normalizedHeaders = normalizeHeaders(rawHeader);
            List<RawCsvRow> rawRows = readRawRows(csvReader, normalizedHeaders);
            Set<String> skuKeys = rawRows.stream()
                    .map(row -> InventoryUtil.trimToNull(row.normalizedValues().get("sku")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Map<String, IngredientStock> existingBySku = ingredientStockRepository.findAllBySkuIn(skuKeys).stream()
                    .collect(Collectors.toMap(IngredientStock::getSku, ingredient -> ingredient, (left, right) -> left));
            SupplierLookup supplierLookup = buildSupplierLookup(restaurantId);
            Map<String, Long> duplicateSkuCounts = rawRows.stream()
                    .map(row -> InventoryUtil.trimToNull(row.normalizedValues().get("sku")))
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(value -> value.toLowerCase(Locale.ROOT), LinkedHashMap::new, Collectors.counting()));

            List<IngredientImportRowResult> rowResults = new ArrayList<>();
            List<IngredientImportPreviewStore.ParsedIngredientRow> validRows = new ArrayList<>();
            int rowsToCreate = 0;
            int rowsToUpdate = 0;

            for (RawCsvRow rawRow : rawRows) {
                Map<String, String> normalizedValues = rawRow.normalizedValues();
                List<String> errors = new ArrayList<>();

                String sku = trimValue(normalizedValues, "sku");
                if (sku == null) {
                    errors.add("sku is required");
                } else if (duplicateSkuCounts.getOrDefault(sku.toLowerCase(Locale.ROOT), 0L) > 1) {
                    errors.add("Duplicate sku found in the import file");
                }

                String ingredientName = trimValue(normalizedValues, "ingredient_name");
                if (ingredientName == null) {
                    errors.add("ingredient_name is required");
                }

                Double totalStock = parseOptionalNonNegativeDouble(normalizedValues, "total_stock", errors);
                Long rowRestaurantId = parseOptionalPositiveLong(normalizedValues, "restaurant_id", errors);
                if (rowRestaurantId != null && !Objects.equals(rowRestaurantId, restaurantId)) {
                    errors.add("restaurant_id must match the selected restaurant");
                }

                Double reorderLevel = parseOptionalNonNegativeDouble(normalizedValues, "reorder_level", errors);
                Boolean isActive = parseOptionalBoolean(normalizedValues, "is_active", errors);
                Long supplierId = parseOptionalPositiveLong(normalizedValues, "supplier_id", errors);
                String supplierName = trimValue(normalizedValues, "supplier_name");
                validateSupplierReference(supplierLookup, supplierId, supplierName, errors);

                IngredientStock existingIngredient = sku == null ? null : existingBySku.get(sku);
                if (existingIngredient != null && !Objects.equals(existingIngredient.getRestaurantId(), restaurantId)) {
                    errors.add("Ingredient with sku " + sku + " belongs to another restaurant");
                }
                if (existingIngredient == null && totalStock == null) {
                    errors.add("total_stock is required for new ingredients");
                }
                if (existingIngredient != null && totalStock != null) {
                    errors.add("total_stock cannot be updated for existing ingredients");
                }

                String action = existingIngredient == null ? "CREATE" : "UPDATE";
                if (!errors.isEmpty()) {
                    rowResults.add(new IngredientImportRowResult(rawRow.rowNumber(), "ERROR", sku, normalizedValues, List.copyOf(errors)));
                    continue;
                }

                validRows.add(new IngredientImportPreviewStore.ParsedIngredientRow(
                        rawRow.rowNumber(),
                        action,
                        sku,
                        ingredientName,
                        trimValue(normalizedValues, "description"),
                        trimValue(normalizedValues, "category"),
                        rowRestaurantId,
                        totalStock,
                        trimValue(normalizedValues, "unit_code"),
                        reorderLevel,
                        supplierId,
                        supplierName,
                        isActive,
                        normalizedValues
                ));
                rowResults.add(new IngredientImportRowResult(rawRow.rowNumber(), action, sku, normalizedValues, List.of()));
                if ("CREATE".equals(action)) {
                    rowsToCreate++;
                } else {
                    rowsToUpdate++;
                }
            }

            rowResults.sort(Comparator.comparingInt(IngredientImportRowResult::rowNumber));
            validRows.sort(Comparator.comparingInt(IngredientImportPreviewStore.ParsedIngredientRow::getRowNumber));
            return new ParsedCsvPreview(rawRows.size(), validRows, rowResults, normalizedHeaders, rowsToCreate, rowsToUpdate);
        } catch (CharacterCodingException exception) {
            throw new AppException("CSV file must be valid UTF-8", HttpStatus.BAD_REQUEST);
        } catch (CsvValidationException exception) {
            throw new AppException("CSV file contains invalid formatting", HttpStatus.BAD_REQUEST);
        } catch (IOException exception) {
            throw new AppException("Unable to read CSV file", HttpStatus.BAD_REQUEST);
        }
    }

    private SupplierLookup buildSupplierLookup(Long restaurantId) {
        List<Supplier> suppliers = supplierRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        Map<Long, Supplier> suppliersById = suppliers.stream()
                .collect(Collectors.toMap(Supplier::getSupplierId, supplier -> supplier, (left, right) -> left));
        Map<String, List<Supplier>> suppliersByName = new HashMap<>();
        for (Supplier supplier : suppliers) {
            String key = supplier.getSupplierName().trim().toLowerCase(Locale.ROOT);
            suppliersByName.computeIfAbsent(key, ignored -> new ArrayList<>()).add(supplier);
        }
        return new SupplierLookup(suppliersById, suppliersByName);
    }

    private List<RawCsvRow> readRawRows(CSVReader csvReader, List<String> normalizedHeaders) throws IOException, CsvValidationException {
        List<RawCsvRow> rawRows = new ArrayList<>();
        String[] line;
        int physicalRowNumber = 1;
        while ((line = csvReader.readNext()) != null) {
            physicalRowNumber++;
            if (isBlankRow(line)) {
                continue;
            }
            if (rawRows.size() >= MAX_ROWS) {
                throw new AppException("CSV row count exceeds the maximum supported limit of " + MAX_ROWS, HttpStatus.BAD_REQUEST);
            }
            Map<String, String> normalizedValues = new LinkedHashMap<>();
            for (int index = 0; index < normalizedHeaders.size(); index++) {
                String header = normalizedHeaders.get(index);
                String value = index < line.length ? line[index] : "";
                normalizedValues.put(header, value == null ? "" : value.trim());
            }
            rawRows.add(new RawCsvRow(physicalRowNumber, Collections.unmodifiableMap(normalizedValues)));
        }
        return rawRows;
    }

    private void validateSupplierReference(SupplierLookup supplierLookup, Long supplierId, String supplierName, List<String> errors) {
        if (supplierId == null && supplierName == null) {
            return;
        }
        Supplier byId = null;
        Supplier byName = null;
        if (supplierId != null) {
            byId = supplierLookup.suppliersById().get(supplierId);
            if (byId == null) {
                errors.add("supplier_id does not belong to the selected restaurant");
            }
        }
        if (supplierName != null) {
            List<Supplier> matches = supplierLookup.suppliersByName().getOrDefault(supplierName.toLowerCase(Locale.ROOT), List.of());
            if (matches.isEmpty()) {
                errors.add("supplier_name does not match any supplier for the selected restaurant");
            } else if (matches.size() > 1) {
                errors.add("supplier_name is ambiguous for the selected restaurant");
            } else {
                byName = matches.get(0);
            }
        }
        if (byId != null && byName != null && !Objects.equals(byId.getSupplierId(), byName.getSupplierId())) {
            errors.add("supplier_id and supplier_name must refer to the same supplier");
        }
    }

    private Supplier resolveSupplier(IngredientImportPreviewStore.ParsedIngredientRow row,
                                     SupplierLookup supplierLookup,
                                     Supplier currentSupplier,
                                     IngredientImportPreviewStore.PreviewSession session) {
        boolean supplierIdHeaderPresent = session.normalizedHeaders().contains("supplier_id");
        boolean supplierNameHeaderPresent = session.normalizedHeaders().contains("supplier_name");
        if (!supplierIdHeaderPresent && !supplierNameHeaderPresent) {
            return currentSupplier;
        }
        if (row.getSupplierId() != null) {
            return supplierLookup.suppliersById().get(row.getSupplierId());
        }
        if (row.getSupplierName() != null) {
            List<Supplier> matches = supplierLookup.suppliersByName().getOrDefault(row.getSupplierName().toLowerCase(Locale.ROOT), List.of());
            if (!matches.isEmpty()) {
                return matches.get(0);
            }
        }
        return session.overwriteNulls() ? null : currentSupplier;
    }

    private String resolveNullableString(String header, String incomingValue, String currentValue, IngredientImportPreviewStore.PreviewSession session) {
        if (!session.normalizedHeaders().contains(header)) {
            return currentValue;
        }
        if (incomingValue != null) {
            return incomingValue;
        }
        return session.overwriteNulls() ? null : currentValue;
    }

    private String resolveStringValue(String header, String incomingValue, String currentValue, String defaultValue, IngredientImportPreviewStore.PreviewSession session) {
        if (!session.normalizedHeaders().contains(header)) {
            return currentValue != null ? currentValue : defaultValue;
        }
        if (incomingValue != null) {
            return incomingValue;
        }
        if (currentValue != null && !session.overwriteNulls()) {
            return currentValue;
        }
        return defaultValue;
    }

    private Double resolveDoubleValue(String header, Double incomingValue, Double currentValue, Double defaultValue, IngredientImportPreviewStore.PreviewSession session) {
        if (!session.normalizedHeaders().contains(header)) {
            return currentValue != null ? currentValue : defaultValue;
        }
        if (incomingValue != null) {
            return incomingValue;
        }
        if (currentValue != null && !session.overwriteNulls()) {
            return currentValue;
        }
        return defaultValue;
    }

    private Boolean resolveBooleanValue(String header, Boolean incomingValue, Boolean currentValue, Boolean defaultValue, IngredientImportPreviewStore.PreviewSession session) {
        if (!session.normalizedHeaders().contains(header)) {
            return currentValue != null ? currentValue : defaultValue;
        }
        if (incomingValue != null) {
            return incomingValue;
        }
        if (currentValue != null && !session.overwriteNulls()) {
            return currentValue;
        }
        return defaultValue;
    }

    private List<String> normalizeHeaders(String[] rawHeader) {
        List<String> normalizedHeaders = new ArrayList<>();
        Set<String> seenHeaders = new HashSet<>();
        for (int index = 0; index < rawHeader.length; index++) {
            String normalizedHeader = rawHeader[index] == null ? "" : rawHeader[index].trim().toLowerCase(Locale.ROOT);
            if (index == 0 && normalizedHeader.startsWith("\uFEFF")) {
                normalizedHeader = normalizedHeader.substring(1);
            }
            if (normalizedHeader.isBlank()) {
                throw new AppException("CSV contains a blank header", HttpStatus.BAD_REQUEST);
            }
            if (!ALLOWED_HEADERS.contains(normalizedHeader)) {
                throw new AppException("Unknown CSV column: " + normalizedHeader, HttpStatus.BAD_REQUEST);
            }
            if (!seenHeaders.add(normalizedHeader)) {
                throw new AppException("Duplicate CSV header: " + normalizedHeader, HttpStatus.BAD_REQUEST);
            }
            normalizedHeaders.add(normalizedHeader);
        }
        if (!normalizedHeaders.contains("sku") || !normalizedHeaders.contains("ingredient_name")) {
            throw new AppException("CSV must include sku and ingredient_name columns", HttpStatus.BAD_REQUEST);
        }
        return List.copyOf(normalizedHeaders);
    }

    private byte[] readValidatedBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException("CSV file is required", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException("CSV file size exceeds the allowed limit", HttpStatus.BAD_REQUEST);
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException("Unable to read CSV file", HttpStatus.BAD_REQUEST);
        }
    }

    private CharsetDecoder utf8Decoder() {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte part : hash) {
                builder.append(String.format("%02x", part));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private Long currentUserId() {
        SecurityUser currentUser = tenantAccessService.currentUser();
        return currentUser.getUserId() == null ? -1L : currentUser.getUserId();
    }

    private boolean isBlankRow(String[] line) {
        for (String value : line) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String trimValue(Map<String, String> values, String header) {
        return InventoryUtil.trimToNull(values.get(header));
    }

    private Double parseOptionalNonNegativeDouble(Map<String, String> values, String header, List<String> errors) {
        String value = trimValue(values, header);
        return value == null ? null : parseNonNegativeDouble(value, header, errors);
    }

    private Double parseNonNegativeDouble(String value, String header, List<String> errors) {
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < 0) {
                errors.add(header + " must be 0 or greater");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.add(header + " must be a valid number");
            return null;
        }
    }

    private Long parseOptionalPositiveLong(Map<String, String> values, String header, List<String> errors) {
        String value = trimValue(values, header);
        if (value == null) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) {
                errors.add(header + " must be greater than 0");
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            errors.add(header + " must be a valid integer");
            return null;
        }
    }

    private Boolean parseOptionalBoolean(Map<String, String> values, String header, List<String> errors) {
        String value = trimValue(values, header);
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (Set.of("true", "1", "yes", "y").contains(normalized)) {
            return true;
        }
        if (Set.of("false", "0", "no", "n").contains(normalized)) {
            return false;
        }
        errors.add(header + " must be true or false");
        return null;
    }

    private record RawCsvRow(int rowNumber, Map<String, String> normalizedValues) {
    }

    private record SupplierLookup(Map<Long, Supplier> suppliersById, Map<String, List<Supplier>> suppliersByName) {
    }

    private record ParsedCsvPreview(int totalRowsRead,
                                    List<IngredientImportPreviewStore.ParsedIngredientRow> validRows,
                                    List<IngredientImportRowResult> rowResults,
                                    List<String> normalizedHeaders,
                                    int rowsToCreate,
                                    int rowsToUpdate) {
        int invalidRowCount() {
            return totalRowsRead - validRows.size();
        }
    }
}
