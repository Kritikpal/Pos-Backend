package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.common.service.FileUploadService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.order.DAO.SaleItem;
import com.kritik.POS.order.repository.SaleItemRepository;
import com.kritik.POS.restaurant.DAO.Category;
import com.kritik.POS.restaurant.DAO.MenuItem;
import com.kritik.POS.restaurant.DAO.ProductFile;
import com.kritik.POS.restaurant.DAO.RestaurantTable;
import com.kritik.POS.restaurant.models.request.CategoryRequest;
import com.kritik.POS.restaurant.models.request.ItemRequest;
import com.kritik.POS.restaurant.models.request.TableRequest;
import com.kritik.POS.restaurant.models.response.CategoryResponse;
import com.kritik.POS.restaurant.models.response.MenuResponse;
import com.kritik.POS.restaurant.models.response.UserDashboard;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.restaurant.service.RestaurantService;
import com.kritik.POS.tax.TaxRate;
import com.kritik.POS.tax.TaxRateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository tableRepository;
    private final CategoryRepository categoryRepository;
    private final SaleItemRepository saleItemRepository;
    private final TaxRateRepository taxRateRepository;
    private final FileUploadService fileUploadService;

    @Autowired
    public RestaurantServiceImpl(MenuItemRepository menuItemRepository, RestaurantTableRepository tableRepository, CategoryRepository categoryRepository, SaleItemRepository saleItemRepository, TaxRateRepository taxRateRepository, FileUploadService fileUploadService) {
        this.menuItemRepository = menuItemRepository;
        this.tableRepository = tableRepository;
        this.categoryRepository = categoryRepository;
        this.saleItemRepository = saleItemRepository;
        this.taxRateRepository = taxRateRepository;
        this.fileUploadService = fileUploadService;
    }

    @Override
    public UserDashboard userDashboard(Integer pageNumber, Integer pageSize, String searchString, Long categoryId) throws AppException {
        List<TaxRate> allByIsActiveTrue = taxRateRepository.findAllByIsActiveTrue();
        List<MenuItem> allByIsActiveOrderByIsTrendingDesc = menuItemRepository.findAllByIsActiveOrderByIsTrendingDesc(true);
        return new UserDashboard(allByIsActiveOrderByIsTrendingDesc, allByIsActiveTrue);
    }

    @Override
    public List<MenuResponse> getMenuItems() throws AppException {
        List<MenuItem> allItems = menuItemRepository.findAll();
        return allItems.stream().map(MenuResponse::buildResponseFromMenu).toList();
    }

    @Override
    public MenuResponse getMenuItemById(Long itemId) throws AppException {
        MenuItem menuItem = menuItemRepository.findById(itemId).orElseThrow(() -> new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST));
        return MenuResponse.buildResponseFromMenu(menuItem);
    }

    @Override
    public MenuResponse addEditMenuItem(ItemRequest itemRequest, MultipartFile productImage) throws AppException {
        MenuItem menuItem = createMenuIteFromRequest(itemRequest);
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
        MenuItem menuItem = menuItemRepository.findById(menuItemId).orElseThrow(() -> new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST));
        List<SaleItem> saleItems = menuItem.getSaleItems().stream().peek(saleItem -> saleItem.setMenuItem(null)).toList();
        saleItemRepository.saveAll(saleItems);
        menuItemRepository.delete(menuItem);
        return true;
    }


    @Transactional
    @Override
    public boolean deleteAllItems() throws AppException {
        for (MenuItem menuItem : menuItemRepository.findAll()) {
            List<SaleItem> saleItems = menuItem.getSaleItems().stream().peek(saleItem -> saleItem.setMenuItem(null)).toList();
            saleItemRepository.saveAll(saleItems);
            menuItemRepository.delete(menuItem);
        }
        categoryRepository.deleteAll();
        return true;
    }

    private MenuItem createMenuIteFromRequest(ItemRequest itemRequest) {
        MenuItem menuItem;
        if (itemRequest.itemId() == null) {
            menuItem = new MenuItem();
        } else {
            menuItem = menuItemRepository.findById(itemRequest.itemId()).orElseThrow(() -> new AppException("Invalid Item Id", HttpStatus.BAD_REQUEST));
        }
        Category category = categoryRepository.findById(itemRequest.categoryId()).orElseThrow(() -> new AppException("Invalid Category Id" + itemRequest.categoryId(), HttpStatus.BAD_GATEWAY));
        return itemRequest.createMenuItemFromRequest(menuItem, category);
    }

    @Override
    public List<RestaurantTable> getAllTables() throws AppException {
        return tableRepository.findAll();
    }

    @Override
    public RestaurantTable getTableById(Long tableId) throws AppException {
        return tableRepository.findById(tableId).orElseThrow(() -> new AppException("Table Id not found", HttpStatus.BAD_REQUEST));
    }

    @Override
    public RestaurantTable addEditTable(TableRequest tableRequest) throws AppException {
        return tableRepository.save(createTableFromRequest(tableRequest));
    }

    @Override
    public boolean deleteTable(Long tableId) throws AppException {
        RestaurantTable table = getTableById(tableId);
        tableRepository.delete(table);
        return true;
    }

    private RestaurantTable createTableFromRequest(TableRequest tableRequest) {
        RestaurantTable restaurantTable = new RestaurantTable();
        if (tableRequest.tableId() != null) {
            restaurantTable = getTableById(tableRequest.tableId());
        }
        restaurantTable.setTableNumber(tableRequest.tableNumber());
        restaurantTable.setSeats(tableRequest.noOfSeat());
        return restaurantTable;
    }


    @Override
    public List<CategoryResponse> getAllCategories() throws AppException {
        return categoryRepository.findAll().stream().map(CategoryResponse::buildCategoryResponse).toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long categoryId) throws AppException {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new AppException("Invalid CategoryId", HttpStatus.BAD_REQUEST));
        return CategoryResponse.buildCategoryResponse(category);
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
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new AppException("Invalid Category id", HttpStatus.BAD_REQUEST));
        for (MenuItem menuItem : category.getMenuItems()) {
            deleteMenuItem(menuItem.getId());
        }
        categoryRepository.deleteById(categoryId);
        return true;
    }

    private Category createCategoryFromRequest(CategoryRequest categoryRequest) {
        Category category = new Category();
        if (categoryRequest.categoryId() != null) {
            category = categoryRepository.findById(categoryRequest.categoryId()).orElseThrow(() -> new AppException("Invalid Category id", HttpStatus.BAD_REQUEST));
        }
        category.setCategoryDescription(categoryRequest.categoryDescription());
        category.setCategoryName(categoryRequest.categoryName());
        return category;
    }
}
