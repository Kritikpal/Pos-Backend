package com.kritik.POS.admin.controller;

import com.kritik.POS.common.service.ExportService;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.kritik.POS.admin.route.AdminRoute.EXPORT_ALL_DATA;
import static com.kritik.POS.admin.route.AdminRoute.GET_LAST5_PAYMENTS;

@RestController
public class ExportController {
    private final ExportService exportService;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantTableRepository tableRepository;

    public ExportController(ExportService exportService, MenuItemRepository menuItemRepository, CategoryRepository categoryRepository, RestaurantTableRepository tableRepository) {
        this.exportService = exportService;
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.tableRepository = tableRepository;
    }


    @Tag(name = "Export")
    @GetMapping(EXPORT_ALL_DATA)
    public boolean ExportAllData(){
        exportService.writeCategoriesToCsv(categoryRepository.findAll());
        exportService.writeProductsToCsv(menuItemRepository.findAll());
        exportService.writeTablesToCsv(tableRepository.findAll());
        return true;
    }



}
