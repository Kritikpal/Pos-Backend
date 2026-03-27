package com.kritik.POS.admin.service.Impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.admin.service.BulkUploadService;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.security.service.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BulkUploadServiceImpl implements BulkUploadService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantService restaurantService;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public BulkUploadServiceImpl(MenuItemRepository menuItemRepository, RestaurantTableRepository tableRepository, CategoryRepository categoryRepository, RestaurantService restaurantService, TenantAccessService tenantAccessService) {
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.categoryRepository = categoryRepository;
        this.restaurantService = restaurantService;
        this.tenantAccessService = tenantAccessService;
    }


    @Override
    public List<MenuResponse> bulkUploadMenuItems(List<ItemRequest> addItemRequest) throws AppException {
        List<MenuItem> menuItems = new ArrayList<>();
        for (ItemRequest itemRequest : addItemRequest) {
            MenuItem menuItem = createMenuItemFromRequest(itemRequest);
            menuItems.add(menuItem);
        }
        List<MenuItem> savedItems = menuItemRepository.saveAll(menuItems);
        return savedItems.stream().map(MenuResponse::buildResponseFromMenu).toList();
    }


    private MenuItem createMenuItemFromRequest(ItemRequest itemRequest) {
        MenuItem menuItem;
        if (itemRequest.itemId() == null) {
            menuItem = new MenuItem();
        } else {
            menuItem = menuItemRepository.findById(itemRequest.itemId()).orElseThrow(() -> new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST));
        }
        Category category = categoryRepository.findById(itemRequest.categoryId()).orElseThrow(() -> new AppException("Invalid Category Id" + itemRequest.categoryId(), HttpStatus.BAD_GATEWAY));
        MenuItem updated = itemRequest.createMenuItemFromRequest(menuItem,category);
        updated.setRestaurantId(category.getRestaurantId());
        if (updated.getItemStock() != null) {
            updated.getItemStock().setRestaurantId(category.getRestaurantId());
        }
        return updated;
    }

    @Override
    public List<RestaurantTable> bulkUploadTables(List<TableRequest> addRestaurantRequestList) throws AppException {
        List<RestaurantTable> restaurantTableList = addRestaurantRequestList.stream().map(this::createTableFromRequest).toList();
        return tableRepository.saveAll(restaurantTableList);
    }


    private RestaurantTable createTableFromRequest(TableRequest tableRequest) {
        RestaurantTable restaurantTable = new RestaurantTable();
        if (tableRequest.tableId() != null) {
            restaurantTable = restaurantService.getTableById(tableRequest.tableId());
        }
        restaurantTable.setTableNumber(tableRequest.tableNumber());
        restaurantTable.setSeats(tableRequest.noOfSeat());
        restaurantTable.setRestaurantId(tenantAccessService.resolveAccessibleRestaurantId(
                tableRequest.restaurantId() != null ? tableRequest.restaurantId() : restaurantTable.getRestaurantId()
        ));
        return restaurantTable;
    }

}
