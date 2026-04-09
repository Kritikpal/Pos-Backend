package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.common.model.PageResponse;
import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.dto.CategoryResponseDto;
import com.kritik.POS.restaurant.dto.MenuItemResponseDto;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.inventory.entity.recipi.MenuItemIngredient;
import com.kritik.POS.restaurant.entity.ProductFile;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.mapper.RestaurantDtoMapper;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.CategoryResponse;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import com.kritik.POS.restaurant.projection.MenuItemSummaryProjection;
import com.kritik.POS.restaurant.projection.UserDashboardMenuItemProjection;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.restaurant.specification.CategorySpecification;
import com.kritik.POS.restaurant.specification.MenuItemSpecification;
import com.kritik.POS.restaurant.specification.RestaurantTableSpecification;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.projection.ActiveTaxRateProjection;
import com.kritik.POS.tax.repository.TaxRateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final TaxRateRepository taxRateRepository;
    private final FileUploadService fileUploadService;
    private final TenantAccessService tenantAccessService;
    private final RestaurantDtoMapper restaurantDtoMapper;
    private final IngredientStockRepository ingredientStockRepository;

    @Override
    public UserDashboard userDashboard(Integer pageNumber, Integer pageSize, String searchString, Long categoryId) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new UserDashboard(List.of(), List.of());
        }

        Page<UserDashboardMenuItemProjection> menuItems = menuItemRepository.findDashboardItems(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                normalizeSearch(searchString),
                categoryId,
                pageable
        );

        List<ActiveTaxRateProjection> taxes = taxRateRepository.findActiveTaxRateSummaries(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
        );

        return new UserDashboard(menuItems.getContent(), taxes);
    }


    @Override
    public PageResponse<MenuItemResponseDto> getMenuItemPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }

        Page<MenuItemSummaryProjection> page = menuItemRepository.findMenuItemSummaries(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                isActive,
                normalizeSearch(search),
                pageable
        );

        return PageResponse.from(page.map(restaurantDtoMapper::toMenuItemDto));
    }

    @Override
    public MenuResponse getMenuItemById(Long itemId) throws AppException {
        return MenuResponse.buildResponseFromMenu(getMenuItemEntity(itemId));
    }

    @Override
    @Transactional
    public MenuResponse addEditMenuItem(ItemRequest itemRequest, MultipartFile productImage) throws AppException {
        MenuItem menuItem = createMenuItemFromRequest(itemRequest);
        if (productImage != null && !productImage.isEmpty()) {
            ProductFile productFile = fileUploadService.uploadFile(productImage);
            menuItem.setProductImage(productFile);
        }
        MenuItem savedMenu = menuItemRepository.save(menuItem);
        return MenuResponse.buildResponseFromMenu(savedMenu);
    }

    @Transactional
    @Override
    public boolean deleteMenuItem(Long menuItemId) throws AppException {
        MenuItem menuItem = getMenuItemEntity(menuItemId);
        List<SaleItem> saleItems = menuItem.getSaleItems().stream().peek(saleItem -> saleItem.setMenuItem(null)).toList();
        saleItemRepository.saveAll(saleItems);
        menuItem.setIsDeleted(true);
        menuItem.setIsActive(false);
        if (menuItem.getItemStock() != null) {
            menuItem.getItemStock().setIsDeleted(true);
            menuItem.getItemStock().setIsActive(false);
        }
        menuItemRepository.save(menuItem);
        return true;
    }

    @Transactional
    @Override
    public boolean deleteAllItems() throws AppException {
        for (MenuItem menuItem : getAccessibleMenuItemEntities()) {
            deleteMenuItem(menuItem.getId());
        }
        for (Category category : getAccessibleCategoryEntities()) {
            category.setIsDeleted(true);
            category.setIsActive(false);
            categoryRepository.save(category);
        }
        return true;
    }

    @Override
    public List<RestaurantTable> getAllTables() throws AppException {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }
        return tableRepository.findVisibleTables(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
        );
    }

    @Override
    public RestaurantTable getTableById(Long tableId) throws AppException {
        return getTableEntity(tableId);
    }

    @Override
    public RestaurantTable addEditTable(TableRequest tableRequest) throws AppException {
        return tableRepository.save(createTableFromRequest(tableRequest));
    }

    @Override
    public boolean deleteTable(Long tableId) throws AppException {
        RestaurantTable table = getTableEntity(tableId);
        table.setIsDeleted(true);
        table.setIsActive(false);
        tableRepository.save(table);
        return true;
    }

    @Override
    public List<CategoryResponse> getAllCategories() throws AppException {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        Specification<Category> specification = Specification.where(CategorySpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            if (accessibleRestaurantIds.isEmpty()) {
                return List.of();
            }
            specification = specification.and(CategorySpecification.belongsToRestaurants(accessibleRestaurantIds));
        }
        return categoryRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt"))
                .stream()
                .map(CategoryResponse::buildCategoryResponse)
                .toList();
    }

    @Override
    public PageResponse<CategoryResponseDto> getCategoryPage(Long chainId, Long restaurantId, Boolean isActive, String search, Integer pageNumber, Integer pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNumber, pageSize, 0, 0, true);
        }
        Page<CategorySummaryProjection> page = categoryRepository.findCategorySummaries(
                tenantAccessService.isSuperAdmin(),
                tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                isActive,
                normalizeSearch(search),
                pageable
        );
        return PageResponse.from(page.map(restaurantDtoMapper::toCategoryDto));
    }

    @Override
    public CategoryResponse getCategoryById(Long categoryId) throws AppException {
        return CategoryResponse.buildCategoryResponse(getCategoryEntity(categoryId));
    }

    @Override
    public CategoryResponse addEditCategory(CategoryRequest categoryRequest) throws AppException {
        Category category = createCategoryFromRequest(categoryRequest);
        Category savedCategory = categoryRepository.save(category);
        return CategoryResponse.buildCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public boolean deleteCategory(Long categoryId) throws AppException {
        Category category = getCategoryEntity(categoryId);
        for (MenuItem menuItem : category.getMenuItems()) {
            if (!Boolean.TRUE.equals(menuItem.getIsDeleted())) {
                deleteMenuItem(menuItem.getId());
            }
        }
        category.setIsDeleted(true);
        category.setIsActive(false);
        categoryRepository.save(category);
        return true;
    }

    private MenuItem createMenuItemFromRequest(ItemRequest itemRequest) {
        MenuItem menuItem = itemRequest.itemId() == null ? new MenuItem() : getMenuItemEntity(itemRequest.itemId());
        Category category = getCategoryEntity(itemRequest.categoryId());
        MenuItem updatedMenuItem = itemRequest.createMenuItemFromRequest(menuItem, category);
        updatedMenuItem.setRestaurantId(category.getRestaurantId());
        updatedMenuItem.setIsDeleted(false);
        replaceIngredientRecipe(updatedMenuItem, itemRequest.recipeBatchSize(), itemRequest.ingredients(), category.getRestaurantId());
        if (!InventoryAvailabilityUtil.hasRecipe(updatedMenuItem)) {
            updatedMenuItem.setIsPrepared(false);
        }
        if (Boolean.TRUE.equals(itemRequest.isPrepared()) && !InventoryAvailabilityUtil.hasRecipe(updatedMenuItem)) {
            throw new AppException("Prepared menu items must also have a recipe", HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(updatedMenuItem.getIsPrepared()) && updatedMenuItem.getPreparedItemStock() != null) {
            updatedMenuItem.getPreparedItemStock().setAvailableQty(0.0);
            updatedMenuItem.getPreparedItemStock().setReservedQty(0.0);
            updatedMenuItem.getPreparedItemStock().setActive(false);
        }
        InventoryUtil.syncMenuAvailability(menuItem);
        return updatedMenuItem;
    }

    private RestaurantTable createTableFromRequest(TableRequest tableRequest) {
        RestaurantTable restaurantTable = tableRequest.tableId() == null ? new RestaurantTable() : getTableEntity(tableRequest.tableId());
        restaurantTable.setTableNumber(tableRequest.tableNumber());
        restaurantTable.setSeats(tableRequest.noOfSeat());
        restaurantTable.setRestaurantId(tenantAccessService.resolveAccessibleRestaurantId(
                tableRequest.restaurantId() != null ? tableRequest.restaurantId() : restaurantTable.getRestaurantId()
        ));
        restaurantTable.setIsDeleted(false);
        return restaurantTable;
    }

    private Category createCategoryFromRequest(CategoryRequest categoryRequest) {
        Category category = categoryRequest.categoryId() == null ? new Category() : getCategoryEntity(categoryRequest.categoryId());
        category.setCategoryDescription(categoryRequest.categoryDescription());
        category.setCategoryName(categoryRequest.categoryName());
        category.setRestaurantId(tenantAccessService.resolveAccessibleRestaurantId(
                categoryRequest.restaurantId() != null ? categoryRequest.restaurantId() : category.getRestaurantId()
        ));
        category.setIsDeleted(false);
        return category;
    }

    private List<MenuItem> getAccessibleMenuItemEntities() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        Specification<MenuItem> specification = Specification.where(MenuItemSpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            if (accessibleRestaurantIds.isEmpty()) {
                return List.of();
            }
            specification = specification.and(MenuItemSpecification.belongsToRestaurants(accessibleRestaurantIds));
        }
        return menuItemRepository.findAll(specification);
    }

    private List<Category> getAccessibleCategoryEntities() {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        Specification<Category> specification = Specification.where(CategorySpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            if (accessibleRestaurantIds.isEmpty()) {
                return List.of();
            }
            specification = specification.and(CategorySpecification.belongsToRestaurants(accessibleRestaurantIds));
        }
        return categoryRepository.findAll(specification);
    }

    private MenuItem getMenuItemEntity(Long itemId) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(null, null);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            throw new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST);
        }
        return menuItemRepository.findDetailedById(
                        itemId,
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)
                )
                .orElseThrow(() -> new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST));
    }

    private Category getCategoryEntity(Long categoryId) {
        Specification<Category> specification = Specification.where(CategorySpecification.hasId(categoryId))
                .and(CategorySpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            specification = specification.and(
                    CategorySpecification.belongsToRestaurants(tenantAccessService.resolveAccessibleRestaurantIds(null, null))
            );
        }
        return categoryRepository.findOne(specification)
                .orElseThrow(() -> new AppException("Invalid Category id", HttpStatus.BAD_REQUEST));
    }

    private RestaurantTable getTableEntity(Long tableId) {
        Specification<RestaurantTable> specification = Specification.where(RestaurantTableSpecification.hasId(tableId))
                .and(RestaurantTableSpecification.notDeleted());
        if (!tenantAccessService.isSuperAdmin()) {
            specification = specification.and(
                    RestaurantTableSpecification.belongsToRestaurants(tenantAccessService.resolveAccessibleRestaurantIds(null, null))
            );
        }
        return tableRepository.findOne(specification)
                .orElseThrow(() -> new AppException("Table Id not found", HttpStatus.BAD_REQUEST));
    }

    private String normalizeSearch(String searchString) {
        return searchString == null ? null : searchString.trim();
    }

    private void replaceIngredientRecipe(MenuItem menuItem,
                                         Integer recipeBatchSize,
                                         List<ItemRequest.IngredientUsageRequest> ingredientRequests,
                                         Long restaurantId) {
        if (ingredientRequests == null && recipeBatchSize == null) {
            return;
        }

        if (menuItem.getIngredientUsages() == null) {
            menuItem.setIngredientUsages(new ArrayList<>());
        }

        if (ingredientRequests == null) {
            if (menuItem.getIngredientUsages().isEmpty()) {
                throw new AppException("Recipe ingredients are required for recipe-based menu items", HttpStatus.BAD_REQUEST);
            }
            MenuRecipe recipe = menuItem.getRecipe() == null ? new MenuRecipe() : menuItem.getRecipe();
            recipe.setMenuItem(menuItem);
            recipe.setBatchSize(recipeBatchSize);
            recipe.setActive(true);
            syncIngredientUsageCollection(recipe.getIngredientUsages(), menuItem.getIngredientUsages());
            menuItem.setRecipe(recipe);
            menuItem.setHasRecipe(true);
            return;
        }

        if (ingredientRequests == null || ingredientRequests.isEmpty()) {
            menuItem.getIngredientUsages().clear();
            if (menuItem.getRecipe() != null) {
                syncIngredientUsageCollection(menuItem.getRecipe().getIngredientUsages(), List.of());
            }
            menuItem.setRecipe(null);
            menuItem.setHasRecipe(false);
            return;
        }

        if (recipeBatchSize == null || recipeBatchSize <= 0) {
            throw new AppException("Recipe batch size is required for recipe-based menu items", HttpStatus.BAD_REQUEST);
        }
        MenuRecipe recipe = menuItem.getRecipe() == null ? new MenuRecipe() : menuItem.getRecipe();
        recipe.setBatchSize(recipeBatchSize);

        Map<String, MenuItemIngredient> existingUsages = new LinkedHashMap<>();
        for (MenuItemIngredient existingUsage : menuItem.getIngredientUsages()) {
            existingUsages.put(existingUsage.getIngredientStock().getSku(), existingUsage);
        }

        Map<String, IngredientStock> ingredientStocksBySku = loadIngredientStocksBySku(restaurantId, ingredientRequests);

        recipe.setMenuItem(menuItem);
        recipe.setActive(true);

        Set<String> addedSkus = new HashSet<>();
        List<MenuItemIngredient> nextUsages = new ArrayList<>();
        for (ItemRequest.IngredientUsageRequest ingredientRequest : ingredientRequests) {
            if (!addedSkus.add(ingredientRequest.ingredientSku())) {
                // Skip duplicate ingredient sku
                continue;
            }
            IngredientStock ingredientStock = ingredientStocksBySku.get(ingredientRequest.ingredientSku());

            MenuItemIngredient ingredientUsage = existingUsages.getOrDefault(ingredientRequest.ingredientSku(), new MenuItemIngredient());
            ingredientUsage.setMenuItem(menuItem);
            ingredientUsage.setRecipe(recipe);
            ingredientUsage.setIngredientStock(ingredientStock);
            ingredientUsage.setQuantityRequired(ingredientRequest.quantityRequired());
            nextUsages.add(ingredientUsage);
        }

        menuItem.getIngredientUsages().clear();
        menuItem.getIngredientUsages().addAll(nextUsages);
        syncIngredientUsageCollection(recipe.getIngredientUsages(), nextUsages);
        menuItem.setRecipe(recipe);
        menuItem.setHasRecipe(!menuItem.getIngredientUsages().isEmpty());

        if (!menuItem.getIngredientUsages().isEmpty() && menuItem.getItemStock() != null) {
            menuItem.getItemStock().setTotalStock(0);
            menuItem.getItemStock().setIsActive(false);
        }
    }

    private Map<String, IngredientStock> loadIngredientStocksBySku(Long restaurantId,
                                                                   List<ItemRequest.IngredientUsageRequest> ingredientRequests) {
        Set<String> requestedSkus = new LinkedHashSet<>();
        for (ItemRequest.IngredientUsageRequest ingredientRequest : ingredientRequests) {
            requestedSkus.add(ingredientRequest.ingredientSku());
        }

        Map<String, IngredientStock> ingredientStocksBySku = new LinkedHashMap<>();
        for (IngredientStock ingredientStock : ingredientStockRepository.findAllBySkuInAndIsDeletedFalse(requestedSkus)) {
            if (!restaurantId.equals(ingredientStock.getRestaurantId())) {
                throw new AppException("Ingredient does not belong to the selected restaurant", HttpStatus.BAD_REQUEST);
            }
            ingredientStocksBySku.put(ingredientStock.getSku(), ingredientStock);
        }

        validateAllIngredientsResolved(requestedSkus, ingredientStocksBySku.keySet());
        return ingredientStocksBySku;
    }

    private void validateAllIngredientsResolved(Collection<String> requestedSkus, Collection<String> resolvedSkus) {
        if (requestedSkus.size() == resolvedSkus.size()) {
            return;
        }
        for (String requestedSku : requestedSkus) {
            if (!resolvedSkus.contains(requestedSku)) {
                throw new AppException("Invalid ingredient sku", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void syncIngredientUsageCollection(List<MenuItemIngredient> managedUsages,
                                               List<MenuItemIngredient> nextUsages) {
        managedUsages.clear();
        managedUsages.addAll(nextUsages);
    }


}
