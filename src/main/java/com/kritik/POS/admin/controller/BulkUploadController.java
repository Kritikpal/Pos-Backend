package com.kritik.POS.admin.controller;

import com.kritik.POS.common.enums.ResponseCode;
import com.kritik.POS.common.model.ApiResponse;
import com.kritik.POS.common.service.CsvService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.DAO.RestaurantTable;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.admin.service.BulkUploadService;
import com.kritik.POS.swagger.SwaggerTags;
import com.opencsv.exceptions.CsvValidationException;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.kritik.POS.restaurant.route.RestaurantRoute.*;

@RestController
//@RequestMapping("/store")
public class BulkUploadController {

    private final BulkUploadService bulkUploadService;
    private final CsvService csvService;

    @Autowired
    public BulkUploadController(BulkUploadService bulkUploadService, CsvService csvService) {
        this.bulkUploadService = bulkUploadService;
        this.csvService = csvService;
    }


    @Tag(name = SwaggerTags.MENU_ITEM)
    @GetMapping(BULK_UPLOAD_MENU_ITEMS)
    public ResponseEntity<ApiResponse<List<MenuResponse>>> addMenuItem() throws AppException, CsvValidationException, IOException {
        List<MenuResponse> menuItem = bulkUploadService.bulkUploadMenuItems(csvService.readProductsFromCsv());
        return ResponseEntity.ok(new ApiResponse<>(menuItem, ResponseCode.SUCCESS, "Success"));
    }

    @Tag(name = SwaggerTags.TABLE)
    @GetMapping(BULK_UPLOAD_TABLES)
    public ResponseEntity<ApiResponse<List<RestaurantTable>>> addTableToRestaurant() throws AppException, CsvValidationException {
        return ResponseEntity.ok(new ApiResponse<>(bulkUploadService.bulkUploadTables(csvService.readTablesFromCsv()), ResponseCode.SUCCESS, "Table added successfully"));
    }

}
