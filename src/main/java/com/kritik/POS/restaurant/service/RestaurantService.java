package com.kritik.POS.restaurant.service;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.MenuUpdateRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.CategoryResponse;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RestaurantService {

    UserDashboard userDashboard(Integer pageNumber, Integer pageSize, String searchString, Long categoryId) throws AppException;

    PageResponse<MenuItemResponseDto> getMenuItemPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) throws AppException;
    MenuResponse getMenuItemById(Long itemId) throws AppException;
    MenuResponse addEditMenuItem(ItemRequest itemRequest, MultipartFile productImage) throws AppException;
    boolean deleteMenuItem(Long menuItemId) throws AppException;
    boolean deleteAllItems() throws AppException;

    List<RestaurantTable> getAllTables() throws AppException;
    RestaurantTable getTableById(Long tableId) throws AppException;
    RestaurantTable addEditTable(TableRequest addRestaurantReq) throws AppException;
    boolean deleteTable(Long tableId) throws AppException;

    List<CategoryResponse> getAllCategories() throws AppException;
    PageResponse<CategoryResponseDto> getCategoryPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) throws AppException;
    CategoryResponse getCategoryById(Long categoryId) throws AppException;
    CategoryResponse addEditCategory(CategoryRequest categoryRequest) throws AppException;
    boolean deleteCategory(Long categoryId) throws AppException;

    MenuResponse updateMenu(@Valid MenuUpdateRequest updateRequest, MultipartFile productImage);
}
