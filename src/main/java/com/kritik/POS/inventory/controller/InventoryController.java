package com.kritik.POS.inventory.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.IngredientImportCommitRequest;
import com.kritik.POS.inventory.models.request.IngredientRequest;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.models.request.PreparedStockUpdateRequest;
import com.kritik.POS.inventory.models.request.ProductionEntryCreateRequest;
import com.kritik.POS.inventory.models.request.StockUpdateRequest;
import com.kritik.POS.inventory.models.request.SupplierRequest;
import com.kritik.POS.inventory.models.response.IngredientImportCommitResponse;
import com.kritik.POS.inventory.models.response.IngredientImportPreviewResponse;
import com.kritik.POS.inventory.models.response.IngredientResponse;
import com.kritik.POS.inventory.models.response.MenuItemIngredientDto;
import com.kritik.POS.inventory.models.response.PreparedStockResponseDto;
import com.kritik.POS.inventory.models.response.ProductionEntryResponseDto;
import com.kritik.POS.inventory.models.response.ProductionEntrySummaryDto;
import com.kritik.POS.inventory.models.response.StockResponse;
import com.kritik.POS.inventory.projection.IngredientStockListProjection;
import com.kritik.POS.inventory.route.InventoryRoute;
import com.kritik.POS.inventory.service.IngredientImportService;
import com.kritik.POS.inventory.service.IngredientService;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.service.PreparedStockService;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.inventory.models.response.SupplierResponse;
import com.kritik.POS.inventory.models.response.SupplierResponseDto;
import com.kritik.POS.inventory.service.ProductionEntryService;
import com.kritik.POS.inventory.service.SupplierService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(InventoryRoute.BASE)
public class InventoryController {

    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final IngredientService ingredientService;
    private final IngredientImportService ingredientImportService;
    private final ProductionEntryService productionEntryService;
    private final PreparedStockService preparedStockService;

    @Tag(name = SwaggerTags.STOCK)
    @GetMapping(InventoryRoute.GET_STOCKS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<StockResponseDto>>> stockPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                inventoryService.getStockPage(chainId, restaurantId, isActive, lowStockOnly, search, page, size)
        ));
    }



    @Tag(name = SwaggerTags.STOCK)
    @GetMapping(InventoryRoute.GET_STOCK)
    public ResponseEntity<ApiResponse<StockResponse>> getStock(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(inventoryService.getStockBySku(sku)));
    }

    @Tag(name = SwaggerTags.STOCK)
    @PostMapping(InventoryRoute.SAVE_STOCK)
    public ResponseEntity<ApiResponse<StockResponse>> saveStock(@RequestBody @Valid ItemStockUpsertRequest itemStockUpsertRequest) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                inventoryService.saveStock(itemStockUpsertRequest),
                "Stock saved successfully"
        ));
    }

    @Tag(name = SwaggerTags.STOCK)
    @PutMapping(InventoryRoute.UPDATE_STOCK)
    public ResponseEntity<ApiResponse<StockResponse>> updateStock(
            @PathVariable String sku,
            @RequestBody @Valid StockUpdateRequest stockUpdateRequest
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                inventoryService.updateStock(sku, stockUpdateRequest),
                "Stock updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.PREPARED_STOCK)
    @GetMapping(InventoryRoute.GET_PREPARED_STOCKS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<PreparedStockResponseDto>>> preparedStockPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                preparedStockService.getPreparedStockPage(chainId, restaurantId, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.PREPARED_STOCK)
    @GetMapping(InventoryRoute.GET_PREPARED_STOCK)
    public ResponseEntity<ApiResponse<PreparedStockResponseDto>> getPreparedStock(@PathVariable Long menuItemId) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(preparedStockService.getPreparedStock(menuItemId)));
    }

    @Tag(name = SwaggerTags.PREPARED_STOCK)
    @PutMapping(InventoryRoute.UPDATE_PREPARED_STOCK)
    public ResponseEntity<ApiResponse<PreparedStockResponseDto>> updatePreparedStock(
            @PathVariable Long menuItemId,
            @RequestBody @Valid PreparedStockUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                preparedStockService.updatePreparedStock(menuItemId, request),
                "Prepared stock updated successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.GET_INGREDIENTS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<IngredientResponse>>> ingredientPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientService.getIngredientPage(chainId, restaurantId, isActive, lowStockOnly, search, page, size)
        ));
    }


    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.GET_INGREDIENTS_PAGE_V2)
    public ResponseEntity<ApiResponse<PageResponse<IngredientStockListProjection>>> ingredientPageV2(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "false") Boolean lowStockOnly,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientService.getIngredientPageV2(chainId, restaurantId, isActive, lowStockOnly, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @GetMapping(InventoryRoute.GET_INGREDIENT)
    public ResponseEntity<ApiResponse<IngredientResponse>> getIngredient(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(ingredientService.getIngredientBySku(sku)));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @PostMapping(InventoryRoute.SAVE_INGREDIENT)
    public ResponseEntity<ApiResponse<IngredientResponse>> saveIngredient(@RequestBody @Valid IngredientRequest ingredientRequest) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientService.saveIngredient(ingredientRequest),
                "Ingredient saved successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @PostMapping(value = InventoryRoute.PREVIEW_INGREDIENT_IMPORT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<IngredientImportPreviewResponse>> previewIngredientImport(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long restaurantId,
            @RequestParam(required = false, defaultValue = "false") Boolean overwriteNulls
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientImportService.previewImport(file, restaurantId, overwriteNulls),
                "Ingredient import preview generated successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @PostMapping(InventoryRoute.COMMIT_INGREDIENT_IMPORT)
    public ResponseEntity<ApiResponse<IngredientImportCommitResponse>> commitIngredientImport(
            @RequestBody @Valid IngredientImportCommitRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientImportService.commitImport(request.previewToken()),
                "Ingredient import committed successfully"
        ));
    }

    @Tag(name = SwaggerTags.INGREDIENT)
    @DeleteMapping(InventoryRoute.DELETE_INGREDIENT)
    public ResponseEntity<ApiResponse<Boolean>> deleteIngredient(@PathVariable String sku) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                ingredientService.deleteIngredient(sku),
                "Ingredient deleted successfully"
        ));
    }

    @Tag(name = SwaggerTags.SUPPLIER)
    @GetMapping(InventoryRoute.GET_SUPPLIERS)
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getSuppliers(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                supplierService.getSuppliers(chainId, restaurantId, isActive)
        ));
    }

    @Tag(name = SwaggerTags.SUPPLIER)
    @GetMapping(InventoryRoute.GET_SUPPLIERS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<SupplierResponseDto>>> supplierPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                supplierService.getSupplierPage(chainId, restaurantId, isActive, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.SUPPLIER)
    @GetMapping(InventoryRoute.GET_SUPPLIER)
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(@PathVariable(name = "id") Long supplierId) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(supplierService.getSupplierById(supplierId)));
    }

    @Tag(name = SwaggerTags.SUPPLIER)
    @PostMapping(InventoryRoute.SAVE_SUPPLIER)
    public ResponseEntity<ApiResponse<SupplierResponse>> saveSupplier(@RequestBody @Valid SupplierRequest supplierRequest) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                supplierService.saveSupplier(supplierRequest),
                "Supplier saved successfully"
        ));
    }

    @Tag(name = SwaggerTags.SUPPLIER)
    @DeleteMapping(InventoryRoute.DELETE_SUPPLIER)
    public ResponseEntity<ApiResponse<Boolean>> deleteSupplier(@PathVariable(name = "id") Long supplierId) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                supplierService.deleteSupplier(supplierId),
                "Supplier deleted successfully"
        ));
    }

    @Tag(name = SwaggerTags.PRODUCTION_ENTRY)
    @GetMapping(InventoryRoute.GET_PRODUCTION_ENTRIES)
    public ResponseEntity<ApiResponse<PageResponse<ProductionEntrySummaryDto>>> productionEntryPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Long menuItemId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                productionEntryService.getProductionEntryPage(chainId, restaurantId, menuItemId, page, size)
        ));
    }

    @Tag(name = SwaggerTags.PRODUCTION_ENTRY)
    @GetMapping(InventoryRoute.GET_PRODUCTION_ENTRY)
    public ResponseEntity<ApiResponse<ProductionEntryResponseDto>> getProductionEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(productionEntryService.getProductionEntry(id)));
    }


    @Tag(name = SwaggerTags.PREPARED_STOCK)
    @GetMapping(InventoryRoute.GET_COOKED_MENUS)
    public ResponseEntity<ApiResponse<PageResponse<PreparedStockResponseDto>>>
    getCookedMenus(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(name = "search", defaultValue = "") String searchString,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "pageNumber must be at least 0") Integer pageNumber,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "pageSize must be at least 1") Integer pageSize
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                preparedStockService.getPreparedStockPage(chainId, restaurantId, searchString, pageNumber, pageSize)
        ));
    }

    @Tag(name = SwaggerTags.PRODUCTION_ENTRY)
    @PostMapping(InventoryRoute.CREATE_PRODUCTION_ENTRY)
    public ResponseEntity<ApiResponse<ProductionEntryResponseDto>> createProductionEntry(
            @RequestBody @Valid ProductionEntryCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                productionEntryService.createProductionEntry(request),
                "Production entry created successfully"
        ));
    }


}
