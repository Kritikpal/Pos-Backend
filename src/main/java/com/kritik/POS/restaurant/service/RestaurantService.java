package com.kritik.POS.restaurant.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.DAO.RestaurantTable;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.CategoryResponse;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RestaurantService {

    UserDashboard userDashboard(Integer pageNumber, Integer pageSize, String searchString, Long categoryId) throws AppException;

//    menuItems
    List<MenuResponse> getMenuItems() throws AppException;
    MenuResponse getMenuItemById(Long itemId) throws AppException;
    MenuResponse addEditMenuItem(ItemRequest itemRequest, MultipartFile productImage) throws AppException;
    boolean deleteMenuItem(Long menuItemId) throws AppException;
    boolean deleteAllItems() throws AppException;

//    tables
    List<RestaurantTable> getAllTables() throws AppException;
    RestaurantTable getTableById(Long tableId) throws AppException;
    RestaurantTable addEditTable(TableRequest addRestaurantReq) throws AppException;
    boolean deleteTable (Long tableId) throws AppException;

//    tables
    List<CategoryResponse> getAllCategories() throws AppException;
    CategoryResponse getCategoryById(Long categoryId) throws AppException;
    CategoryResponse addEditCategory(CategoryRequest categoryRequest) throws AppException;
    boolean deleteCategory (Long categoryId) throws AppException;
}
