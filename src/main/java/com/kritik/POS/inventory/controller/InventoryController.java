package com.kritik.POS.inventory.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.inventory.models.request.ItemStockUpsertRequest;
import com.kritik.POS.inventory.route.InventoryRoute;
import com.kritik.POS.inventory.service.IngredientService;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.models.response.StockReceiptResponseDto;
import com.kritik.POS.inventory.models.response.StockResponseDto;
import com.kritik.POS.inventory.models.response.SupplierResponseDto;
import com.kritik.POS.inventory.service.ReceiptService;
import com.kritik.POS.inventory.service.SupplierService;
import com.kritik.POS.restaurant.models.request.IngredientRequest;
import com.kritik.POS.restaurant.models.request.StockReceiptCreateRequest;
import com.kritik.POS.restaurant.models.request.StockUpdateRequest;
import com.kritik.POS.restaurant.models.request.SupplierRequest;
import com.kritik.POS.restaurant.models.response.IngredientResponse;
import com.kritik.POS.restaurant.models.response.StockReceiptResponse;
import com.kritik.POS.restaurant.models.response.StockResponse;
import com.kritik.POS.restaurant.models.response.SupplierResponse;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping(InventoryRoute.BASE)
public class InventoryController {

    private final InventoryService inventoryService;
    private final SupplierService supplierService;
    private final IngredientService ingredientService;

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


}
