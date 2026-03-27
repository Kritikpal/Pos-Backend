package com.kritik.POS.admin.controller;

import com.kritik.POS.common.service.ExportService;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.restaurant.specification.CategorySpecification;
import com.kritik.POS.restaurant.specification.MenuItemSpecification;
import com.kritik.POS.security.service.TenantAccessService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.kritik.POS.admin.route.AdminRoute.EXPORT_ALL_DATA;

@RestController
public class ExportController {
    private final ExportService exportService;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantTableRepository tableRepository;
    private final TenantAccessService tenantAccessService;

    public ExportController(ExportService exportService, MenuItemRepository menuItemRepository, CategoryRepository categoryRepository, RestaurantTableRepository tableRepository, TenantAccessService tenantAccessService) {
        this.exportService = exportService;
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.tableRepository = tableRepository;
        this.tenantAccessService = tenantAccessService;
    }


    @Tag(name = "Export")
    @GetMapping(EXPORT_ALL_DATA)
    public boolean ExportAllData(){
        var accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        Specification<Category> categorySpecification = Specification.where(CategorySpecification.notDeleted());
        Specification<MenuItem> menuItemSpecification = Specification.where(MenuItemSpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            categorySpecification = categorySpecification.and(CategorySpecification.belongsToRestaurants(accessibleRestaurantIds));
            menuItemSpecification = menuItemSpecification.and(MenuItemSpecification.belongsToRestaurants(accessibleRestaurantIds));
        }
        List<RestaurantTable> tables = tableRepository.findVisibleTables(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
        );
        exportService.writeCategoriesToCsv(categoryRepository.findAll(categorySpecification));
        exportService.writeProductsToCsv(menuItemRepository.findAll(menuItemSpecification));
        exportService.writeTablesToCsv(tables);
        return true;
    }



}
