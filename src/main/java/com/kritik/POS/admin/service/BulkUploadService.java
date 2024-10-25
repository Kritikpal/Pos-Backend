package com.kritik.POS.admin.service;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.DAO.RestaurantTable;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.MenuResponse;

import java.util.List;

public interface BulkUploadService {
    List<RestaurantTable> bulkUploadTables(List<TableRequest> addRestaurantReq) throws AppException;
    List<MenuResponse> bulkUploadMenuItems(List<ItemRequest> itemRequest) throws AppException;

}
