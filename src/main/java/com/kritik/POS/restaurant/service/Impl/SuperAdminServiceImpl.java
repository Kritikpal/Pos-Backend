package com.kritik.POS.restaurant.service.Impl;

import com.kritik.POS.exception.errors.BadRequestException;
import com.kritik.POS.inventory.entity.IngredientStock;
import com.kritik.POS.inventory.entity.StockReceipt;
import com.kritik.POS.inventory.entity.Supplier;
import com.kritik.POS.inventory.repository.IngredientStockRepository;
import com.kritik.POS.inventory.repository.StockReceiptRepository;
import com.kritik.POS.inventory.repository.SupplierRepository;
import com.kritik.POS.invoice.repository.InvoiceRepository;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.restaurant.dto.RestaurantChainResponseDto;
import com.kritik.POS.restaurant.dto.RestaurantDataDeletionResponseDto;
import com.kritik.POS.restaurant.dto.RestaurantDetailResponseDto;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.models.request.RestaurantChainRequest;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.models.request.RestaurantSetupRequest;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import com.kritik.POS.restaurant.models.response.RestaurantSetupResponse;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantChainRepository;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.restaurant.service.SuperAdminService;
import com.kritik.POS.restaurant.util.RestaurantMapper;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.repository.TaxRateRepository;
import com.kritik.POS.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantChainRepository chainRepository;
    private final UserService userService;
    private final TenantAccessService tenantAccessService;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final IngredientStockRepository ingredientStockRepository;
    private final SupplierRepository supplierRepository;
    private final StockReceiptRepository stockReceiptRepository;
    private final TaxRateRepository taxRateRepository;
    private final OrderRepository orderRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurantSetup(RestaurantSetupRequest req) {
        tenantAccessService.assertSuperAdmin();
        userService.validateUserNotExists(req.getAdminEmail());

        RestaurantChain chain = RestaurantMapper.toChain(req);
        chain = chainRepository.save(chain);

        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists in this chain");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        userService.createRestaurantAdmin(
                chain.getChainId(),
                restaurant.getRestaurantId(),
                req.getAdminEmail(),
                req.getAdminPhone(),
                req.getAdminPassword()
        );

        return RestaurantMapper.buildResponse(chain, restaurant, req.getAdminEmail());
    }

    @Override
    public Long createChain(String chainName) {
        tenantAccessService.assertSuperAdmin();
        if (chainRepository.existsByName(chainName)) {
            throw new BadRequestException("Chain already exists");
        }

        RestaurantChain chain = new RestaurantChain();
        chain.setName(chainName);

        return chainRepository.save(chain).getChainId();
    }

    @Override
    public Page<RestaurantChainInfo> getAllChains(Long chainId, String search, Pageable pageable) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        return chainRepository.findByChainIdOrNameIgnoreCase(accessibleChainId, search, pageable);
    }

    @Override
    public RestaurantChainResponseDto getChain(Long chainId) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        return toChainResponse(getChainEntity(accessibleChainId));
    }

    @Override
    @Transactional
    public RestaurantChainResponseDto updateChain(Long chainId, RestaurantChainRequest request) {
        Long manageableChainId = tenantAccessService.resolveManageableChainId(chainId);
        RestaurantChain chain = getChainEntity(manageableChainId);

        if (chainRepository.existsByNameIgnoreCaseAndChainIdNot(request.getName(), manageableChainId)) {
            throw new BadRequestException("Chain already exists");
        }

        chain.setName(request.getName().trim());
        chain.setDescription(request.getDescription());
        chain.setLogoUrl(request.getLogoUrl());
        chain.setEmail(request.getEmail());
        chain.setPhoneNumber(request.getPhoneNumber());
        chain.setGstNumber(request.getGstNumber());
        if (request.getIsActive() != null) {
            chain.setActive(request.getIsActive());
        }

        return toChainResponse(chainRepository.save(chain));
    }

    @Override
    @Transactional
    public RestaurantSetupResponse createRestaurant(RestaurantRequest req, Long chainId) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleChainId = tenantAccessService.resolveManageableChainId(chainId);
        if (accessibleChainId == null) {
            throw new BadRequestException("Chain id is required");
        }

        RestaurantChain chain = chainRepository.findById(accessibleChainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        if (restaurantRepository.existsByCodeAndChain(req.getCode(), chain)) {
            throw new BadRequestException("Restaurant code already exists");
        }

        Restaurant restaurant = RestaurantMapper.toRestaurant(req, chain);
        restaurant = restaurantRepository.save(restaurant);

        return RestaurantMapper.buildResponse(chain, restaurant, null);
    }

    @Override
    public Page<RestaurantProjection> getAllRestaurants(Long chainId, Long restaurantId, Boolean isActive, String search, Pageable pageable) {
        Long accessibleChainId = tenantAccessService.resolveAccessibleChainId(chainId);
        if (!tenantAccessService.isSuperAdmin() && tenantAccessService.currentUser().getRestaurantId() != null) {
            restaurantId = tenantAccessService.currentUser().getRestaurantId();
        }
        return restaurantRepository.findRestaurants(accessibleChainId, search, restaurantId, isActive, pageable);
    }

    @Override
    public RestaurantDetailResponseDto getRestaurant(Long restaurantId) {
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        return toRestaurantResponse(getRestaurantEntity(accessibleRestaurantId));
    }

    @Override
    @Transactional
    public RestaurantDetailResponseDto updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(restaurantId);
        Restaurant restaurant = getRestaurantEntity(manageableRestaurantId);
        String nextCode = resolveRestaurantCode(request, restaurant.getCode());

        if (restaurantRepository.existsByCodeAndChainAndRestaurantIdNot(nextCode, restaurant.getChain(), manageableRestaurantId)) {
            throw new BadRequestException("Restaurant code already exists");
        }

        restaurant.setName(request.getRestaurantName().trim());
        restaurant.setCode(nextCode);
        restaurant.setAddressLine1(request.getAddressLine1());
        restaurant.setAddressLine2(request.getAddressLine2());
        restaurant.setCity(request.getCity());
        restaurant.setState(request.getState());
        restaurant.setCountry(request.getCountry());
        restaurant.setPincode(request.getPincode());
        restaurant.setPhoneNumber(request.getRestaurantPhone());
        restaurant.setEmail(request.getRestaurantEmail());
        restaurant.setGstNumber(request.getRestaurantGstNumber());
        if (request.getIsActive() != null) {
            restaurant.setActive(request.getIsActive());
        }

        return toRestaurantResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantDataDeletionResponseDto deleteRestaurantOperationalData(Long restaurantId) {
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(restaurantId);
        Restaurant restaurant = getRestaurantEntity(manageableRestaurantId);

        var menus = menuItemRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var categories = categoryRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var tables = restaurantTableRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var ingredients = ingredientStockRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var suppliers = supplierRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var receipts = stockReceiptRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var taxes = taxRateRepository.findAllByRestaurantIdAndIsDeletedFalse(manageableRestaurantId);
        var orders = orderRepository.findAllVisibleByRestaurantIdWithDetails(manageableRestaurantId);
        long deletedInvoiceCount = invoiceRepository.deleteByOrderRestaurantId(manageableRestaurantId);

        softDeleteMenus(menus);
        softDeleteCategories(categories);
        softDeleteTables(tables);
        softDeleteIngredients(ingredients);
        softDeleteSuppliers(suppliers);
        softDeleteReceipts(receipts);
        softDeleteTaxes(taxes);
        softDeleteOrders(orders);

        return new RestaurantDataDeletionResponseDto(
                restaurant.getRestaurantId(),
                restaurant.getName(),
                menus.size(),
                categories.size(),
                tables.size(),
                ingredients.size(),
                suppliers.size(),
                receipts.size(),
                taxes.size(),
                orders.size(),
                (int) deletedInvoiceCount
        );
    }

    @Override
    public void createChainAdmin(Long chainId, String email, String phone) {
        tenantAccessService.assertSuperAdmin();
        RestaurantChain chain = chainRepository.findById(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));

        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userService.createChainAdmin(
                chain.getChainId(),
                email,
                phone,
                password
        );
    }

    @Override
    public void createRestaurantAdmin(Long restaurantId, String email, String phone) {
        tenantAccessService.assertChainAdminOrSuperAdmin();
        Long accessibleRestaurantId = tenantAccessService.resolveAccessibleRestaurantId(restaurantId);
        Restaurant restaurant = restaurantRepository.findByRestaurantIdAndIsDeletedFalse(accessibleRestaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));

        String password = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        userService.createRestaurantAdmin(
                restaurant.getChainId(),
                restaurant.getRestaurantId(),
                email,
                phone,
                password
        );
    }

    private RestaurantChain getChainEntity(Long chainId) {
        return chainRepository.findByChainIdAndIsDeletedFalse(chainId)
                .orElseThrow(() -> new BadRequestException("Chain not found"));
    }

    private Restaurant getRestaurantEntity(Long restaurantId) {
        return restaurantRepository.findDetailByRestaurantId(restaurantId)
                .orElseThrow(() -> new BadRequestException("Restaurant not found"));
    }

    private RestaurantChainResponseDto toChainResponse(RestaurantChain chain) {
        return new RestaurantChainResponseDto(
                chain.getChainId(),
                chain.getName(),
                chain.getDescription(),
                chain.getLogoUrl(),
                chain.getEmail(),
                chain.getPhoneNumber(),
                chain.getGstNumber(),
                chain.isActive()
        );
    }

    private RestaurantDetailResponseDto toRestaurantResponse(Restaurant restaurant) {
        return new RestaurantDetailResponseDto(
                restaurant.getChainId(),
                restaurant.getChain().getName(),
                restaurant.getRestaurantId(),
                restaurant.getName(),
                restaurant.getCode(),
                restaurant.getAddressLine1(),
                restaurant.getAddressLine2(),
                restaurant.getCity(),
                restaurant.getState(),
                restaurant.getCountry(),
                restaurant.getPincode(),
                restaurant.getPhoneNumber(),
                restaurant.getEmail(),
                restaurant.getGstNumber(),
                restaurant.getCurrency(),
                restaurant.getTimezone(),
                restaurant.isActive()
        );
    }

    private String resolveRestaurantCode(RestaurantRequest request, String fallbackCode) {
        if (StringUtils.hasText(request.getCode())) {
            return request.getCode().trim();
        }
        return fallbackCode;
    }

    private void softDeleteMenus(java.util.List<MenuItem> menus) {
        for (MenuItem menuItem : menus) {
            menuItem.setIsDeleted(true);
            menuItem.setIsActive(false);
            menuItem.setIsAvailable(false);
            menuItem.getIngredientUsages().clear();
            menuItem.setRecipe(null);
            menuItem.setHasRecipe(false);
            if (menuItem.getItemStock() != null) {
                menuItem.getItemStock().setIsDeleted(true);
                menuItem.getItemStock().setIsActive(false);
                menuItem.getItemStock().setTotalStock(0);
            }
        }
        menuItemRepository.saveAll(menus);
    }

    private void softDeleteCategories(java.util.List<Category> categories) {
        for (Category category : categories) {
            category.setIsDeleted(true);
            category.setIsActive(false);
        }
        categoryRepository.saveAll(categories);
    }

    private void softDeleteTables(java.util.List<RestaurantTable> tables) {
        for (RestaurantTable table : tables) {
            table.setIsDeleted(true);
            table.setIsActive(false);
        }
        restaurantTableRepository.saveAll(tables);
    }

    private void softDeleteIngredients(java.util.List<IngredientStock> ingredients) {
        for (IngredientStock ingredient : ingredients) {
            ingredient.setIsDeleted(true);
            ingredient.setIsActive(false);
            ingredient.setTotalStock(0.0);
        }
        ingredientStockRepository.saveAll(ingredients);
    }

    private void softDeleteSuppliers(java.util.List<Supplier> suppliers) {
        for (Supplier supplier : suppliers) {
            supplier.setIsDeleted(true);
            supplier.setIsActive(false);
        }
        supplierRepository.saveAll(suppliers);
    }

    private void softDeleteReceipts(java.util.List<StockReceipt> receipts) {
        for (StockReceipt receipt : receipts) {
            receipt.setIsDeleted(true);
        }
        stockReceiptRepository.saveAll(receipts);
    }

    private void softDeleteTaxes(java.util.List<TaxRate> taxes) {
        for (TaxRate tax : taxes) {
            tax.setDeleted(true);
            tax.setActive(false);
        }
        taxRateRepository.saveAll(taxes);
    }

    private void softDeleteOrders(java.util.List<Order> orders) {
        for (Order order : orders) {
            order.setDeleted(true);
            order.setActive(false);
            for (SaleItem saleItem : order.getOrderItemList()) {
                saleItem.setDeleted(true);
                saleItem.setActive(false);
            }
        }
        orderRepository.saveAll(orders);
    }
}
