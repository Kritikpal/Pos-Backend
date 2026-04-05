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
import com.kritik.POS.restaurant.dto.RestaurantDataDeletionResponseDto;
import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.entity.RestaurantTable;
import com.kritik.POS.restaurant.models.request.RestaurantChainRequest;
import com.kritik.POS.restaurant.models.request.RestaurantRequest;
import com.kritik.POS.restaurant.repository.CategoryRepository;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantChainRepository;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.restaurant.repository.RestaurantTableRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.repository.TaxRateRepository;
import com.kritik.POS.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantChainRepository chainRepository;

    @Mock
    private UserService userService;

    @Mock
    private TenantAccessService tenantAccessService;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private IngredientStockRepository ingredientStockRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private StockReceiptRepository stockReceiptRepository;

    @Mock
    private TaxRateRepository taxRateRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private SuperAdminServiceImpl superAdminService;

    @Test
    void updateRestaurantUsesManageableRestaurantIdAndPersistsChanges() {
        RestaurantChain chain = new RestaurantChain();
        chain.setChainId(11L);
        chain.setName("North Kitchens");

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(21L);
        restaurant.setChain(chain);
        restaurant.setChainId(11L);
        restaurant.setName("Old Name");
        restaurant.setCode("OLD001");
        restaurant.setCity("Delhi");
        restaurant.setCurrency("INR");
        restaurant.setTimezone("Asia/Kolkata");
        restaurant.setActive(true);

        RestaurantRequest request = new RestaurantRequest();
        request.setRestaurantName("New Name");
        request.setCode("NEW001");
        request.setCity("Noida");
        request.setRestaurantPhone("9876543210");
        request.setRestaurantEmail("admin@example.com");
        request.setIsActive(false);

        when(tenantAccessService.resolveManageableRestaurantId(21L)).thenReturn(21L);
        when(restaurantRepository.findDetailByRestaurantId(21L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.existsByCodeAndChainAndRestaurantIdNot("NEW001", chain, 21L)).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = superAdminService.updateRestaurant(21L, request);

        assertEquals(21L, response.restaurantId());
        assertEquals("New Name", response.restaurantName());
        assertEquals("NEW001", response.code());
        assertEquals("Noida", response.city());
        assertEquals("North Kitchens", response.chainName());
        verify(tenantAccessService).resolveManageableRestaurantId(21L);
    }

    @Test
    void updateChainRejectsDuplicateName() {
        RestaurantChain chain = new RestaurantChain();
        chain.setChainId(7L);
        chain.setName("Existing");
        chain.setActive(true);

        RestaurantChainRequest request = new RestaurantChainRequest();
        request.setName("Duplicate");

        when(tenantAccessService.resolveManageableChainId(7L)).thenReturn(7L);
        when(chainRepository.findByChainIdAndIsDeletedFalse(7L)).thenReturn(Optional.of(chain));
        when(chainRepository.existsByNameIgnoreCaseAndChainIdNot("Duplicate", 7L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> superAdminService.updateChain(7L, request));

        verify(chainRepository, never()).save(any(RestaurantChain.class));
    }

    @Test
    void deleteRestaurantOperationalDataKeepsRestaurantAndReturnsDeletionCounts() {
        RestaurantChain chain = new RestaurantChain();
        chain.setChainId(11L);
        chain.setName("North Kitchens");

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(21L);
        restaurant.setName("Branch A");
        restaurant.setChain(chain);
        restaurant.setChainId(11L);
        restaurant.setActive(true);

        MenuItem menuItem = new MenuItem();
        menuItem.setId(100L);
        menuItem.setIngredientUsages(new java.util.ArrayList<>());

        Category category = new Category();
        RestaurantTable table = new RestaurantTable();
        IngredientStock ingredientStock = new IngredientStock();
        Supplier supplier = new Supplier();
        StockReceipt receipt = new StockReceipt();
        TaxRate taxRate = new TaxRate();
        Order order = new Order();
        order.setOrderItemList(new java.util.ArrayList<>());
        order.getOrderItemList().add(new SaleItem());

        when(tenantAccessService.resolveManageableRestaurantId(21L)).thenReturn(21L);
        when(restaurantRepository.findDetailByRestaurantId(21L)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(menuItem));
        when(categoryRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(category));
        when(restaurantTableRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(table));
        when(ingredientStockRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(ingredientStock));
        when(supplierRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(supplier));
        when(stockReceiptRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(receipt));
        when(taxRateRepository.findAllByRestaurantIdAndIsDeletedFalse(21L)).thenReturn(java.util.List.of(taxRate));
        when(orderRepository.findAllVisibleByRestaurantIdWithDetails(21L)).thenReturn(java.util.List.of(order));
        when(invoiceRepository.deleteByOrderRestaurantId(21L)).thenReturn(2L);

        RestaurantDataDeletionResponseDto response = superAdminService.deleteRestaurantOperationalData(21L);

        assertEquals(21L, response.restaurantId());
        assertEquals("Branch A", response.restaurantName());
        assertEquals(1, response.deletedMenuCount());
        assertEquals(1, response.deletedOrderCount());
        assertEquals(2, response.deletedInvoiceCount());
        verify(tenantAccessService).resolveManageableRestaurantId(21L);
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }
}
