package com.kritik.POS.restaurant.controller;

import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.MenuUpdateRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.CategoryResponse;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.route.RestaurantRoute;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.swagger.SwaggerTags;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class StaffController {

    private final RestaurantService restaurantService;



    @Tag(name = SwaggerTags.MENU_ITEM)
    @GetMapping(RestaurantRoute.GET_MENU_ITEMS_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponseDto>>> menuItemPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                restaurantService.getMenuItemPage(chainId, restaurantId, isActive, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.MENU_ITEM)
    @GetMapping(RestaurantRoute.GET_MENU_ITEM)
    public ResponseEntity<ApiResponse<MenuResponse>> getMenuItem(@PathVariable(name = "id") Long meniItemId) {
        MenuResponse menuItem = restaurantService.getMenuItemById(meniItemId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(menuItem));
    }

    @Tag(name = SwaggerTags.MENU_ITEM)
    @PostMapping(
            value = RestaurantRoute.EDIT_ADD_MENU_ITEM,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MenuResponse>> editMenuItem(
            @RequestPart("itemRequest") @Valid ItemRequest itemRequest,
            @RequestPart(value = "productImage", required = false) MultipartFile productImage
    ) {
        MenuResponse savedMenuItem = restaurantService.addEditMenuItem(itemRequest, productImage);
        return ResponseEntity.ok(ApiResponse.SUCCESS(savedMenuItem));
    }

    @Tag(name = SwaggerTags.MENU_ITEM)
    @PostMapping(
            value = RestaurantRoute.UPDATE_MENU_ITEM,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<MenuResponse>> updateMenuItem(
            @RequestPart("itemRequest") @Valid MenuUpdateRequest updateRequest,
            @RequestPart(value = "productImage", required = false) MultipartFile productImage
    ) {
        MenuResponse savedMenuItem = restaurantService.updateMenu(updateRequest, productImage);
        return ResponseEntity.ok(ApiResponse.SUCCESS(savedMenuItem));
    }

    @Tag(name = SwaggerTags.MENU_ITEM)
    @DeleteMapping(RestaurantRoute.DELETE_MENU_ITEM)
    public ResponseEntity<ApiResponse<Boolean>> deleteMenuItem(@PathVariable(name = "id") Long itemId) {
        boolean savedMenuItem = restaurantService.deleteMenuItem(itemId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(savedMenuItem, "Successfully Deleted"));
    }

    @Tag(name = SwaggerTags.MENU_ITEM)
    @DeleteMapping(RestaurantRoute.DELETE_ALL_MENU_ITEMS)
    public ResponseEntity<ApiResponse<Boolean>> deleteAllMenuItems() {
        boolean savedMenuItem = restaurantService.deleteAllItems();
        return ResponseEntity.ok(ApiResponse.SUCCESS(savedMenuItem, "Successfully Deleted All Items"));
    }


    @Tag(name = SwaggerTags.CATEGORY)
    @GetMapping(RestaurantRoute.GET_ALL_CATEGORIES)
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> menuItemList = restaurantService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.SUCCESS(menuItemList));
    }

    @Tag(name = SwaggerTags.CATEGORY)
    @GetMapping(RestaurantRoute.GET_CATEGORIES_PAGE)
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponseDto>>> categoryPage(
            @RequestParam(required = false) Long chainId,
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1") Integer size
    ) {
        return ResponseEntity.ok(ApiResponse.SUCCESS(
                restaurantService.getCategoryPage(chainId, restaurantId, isActive, search, page, size)
        ));
    }

    @Tag(name = SwaggerTags.CATEGORY)
    @GetMapping(RestaurantRoute.GET_CATEGORY)
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable(name = "id") Long categoryId) {
        CategoryResponse category = restaurantService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(category));
    }

    @Tag(name = SwaggerTags.CATEGORY)
    @PostMapping(RestaurantRoute.EDIT_ADD_CATEGORY)
    public ResponseEntity<ApiResponse<CategoryResponse>> editCategory(@RequestBody @Valid CategoryRequest categoryRequest) {
        CategoryResponse saved = restaurantService.addEditCategory(categoryRequest);
        return ResponseEntity.ok(ApiResponse.SUCCESS(saved));
    }

    @Tag(name = SwaggerTags.CATEGORY)
    @DeleteMapping(RestaurantRoute.DELETE_CATEGORY)
    public ResponseEntity<ApiResponse<Boolean>> deleteCategory(@PathVariable(name = "id") Long categoryId) {
        boolean deletedCategory = restaurantService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(deletedCategory, "Successfully Deleted"));
    }


    @Tag(name = SwaggerTags.TABLE)
    @GetMapping(RestaurantRoute.GET_ALL_TABLES)
    public ResponseEntity<ApiResponse<List<RestaurantTable>>> getAllTables() {
        List<RestaurantTable> allTables = restaurantService.getAllTables();
        return ResponseEntity.ok(ApiResponse.SUCCESS(allTables));
    }

    @Tag(name = SwaggerTags.TABLE)
    @GetMapping(RestaurantRoute.GET_TABLE)
    public ResponseEntity<ApiResponse<RestaurantTable>> getTableById(@PathVariable(name = "id") Long tableId) {
        RestaurantTable table = restaurantService.getTableById(tableId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(table));
    }

    @Tag(name = SwaggerTags.TABLE)
    @PostMapping(RestaurantRoute.EDIT_ADD_TABLE)
    public ResponseEntity<ApiResponse<RestaurantTable>> addEditTable(@RequestBody @Valid TableRequest tableRequest) {
        RestaurantTable savedTable = restaurantService.addEditTable(tableRequest);
        return ResponseEntity.ok(ApiResponse.SUCCESS(savedTable));
    }

    @Tag(name = SwaggerTags.TABLE)
    @DeleteMapping(RestaurantRoute.DELETE_TABLE)
    public ResponseEntity<ApiResponse<Boolean>> deleteTable(@PathVariable(name = "id") Long tableId) {
        boolean deletedCategory = restaurantService.deleteTable(tableId);
        return ResponseEntity.ok(ApiResponse.SUCCESS(deletedCategory, "Successfully Deleted"));
    }
}
