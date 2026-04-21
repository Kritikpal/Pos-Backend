package com.kritik.POS.order.service.Impl;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.service.TaxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguredOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ConfiguredSaleItemRepository configuredSaleItemRepository;

    @Mock
    private ConfiguredMenuTemplateRepository configuredMenuTemplateRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TaxService taxService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private ConfiguredOrderServiceImpl configuredOrderService;

    @Test
    void initiateOrderAppliesPriceDeltaOnlyAboveMinimumQuantityForConfiguredOrders() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 180.0, null);
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = quantityTemplate(parent, roti, naan);

        List<ConfiguredSaleItem> persistedItems = new ArrayList<>();
        when(configuredMenuTemplateRepository.findById(700L)).thenReturn(Optional.of(template));
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(901L);
            return order;
        });
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedItems.clear();
            persistedItems.addAll(items);
            return items;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(901L)).thenAnswer(invocation -> persistedItems);

        ConfiguredOrderResponse response = configuredOrderService.initiateOrder(request());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).unitPrice()).isEqualTo(200.0);
        assertThat(response.getItems().get(0).lineTotal()).isEqualTo(200.0);
        assertThat(response.getItems().get(0).items())
                .extracting(item -> item.quantity())
                .containsExactlyInAnyOrder(3, 1);

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 3),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 1)
                );
    }

    @Test
    void initiateOrderSupportsExactOneAndMixableSelectionSlotsForConfiguredOrders() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 180.0, null);
        MenuItem rice = menuItem(30L, 101L, "Rice", MenuType.DIRECT, 0.0, "RICE-SKU");
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = mixedTemplate(parent, rice, roti, naan);

        List<ConfiguredSaleItem> persistedItems = new ArrayList<>();
        when(configuredMenuTemplateRepository.findById(702L)).thenReturn(Optional.of(template));
        when(inventoryService.getAccessibleMenuItem(30L)).thenReturn(rice);
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(902L);
            return order;
        });
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedItems.clear();
            persistedItems.addAll(items);
            return items;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(902L)).thenAnswer(invocation -> persistedItems);

        ConfiguredOrderResponse response = configuredOrderService.initiateOrder(mixedRequest());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).unitPrice()).isEqualTo(195.0);
        assertThat(response.getItems().get(0).items())
                .extracting(item -> item.childItemName(), item -> item.quantity())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Rice", 1),
                        org.assertj.core.groups.Tuple.tuple("Roti", 2),
                        org.assertj.core.groups.Tuple.tuple("Naan", 2)
                );

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("RICE-SKU", 1),
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 2),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 2)
                );
    }

    @Test
    void initiateOrderRejectsSelectedQuantityBelowConfiguredMinimum() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 180.0, null);
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = quantityTemplate(parent, roti, naan);

        when(configuredMenuTemplateRepository.findById(700L)).thenReturn(Optional.of(template));

        ConfiguredOrderInitiateRequest request = new ConfiguredOrderInitiateRequest();
        request.setOrderItems(List.of(new ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest(
                700L,
                1,
                List.of(new ConfiguredOrderInitiateRequest.SlotItemRequest(100L, List.of(
                        new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(20L, 1)
                )))
        )));

        assertThatThrownBy(() -> configuredOrderService.initiateOrder(request))
                .hasMessageContaining("Roti requires at least 2 quantity");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderRejectsTooManySelectedItemsForSlot() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 180.0, null);
        MenuItem rice = menuItem(30L, 101L, "Rice", MenuType.DIRECT, 0.0, "RICE-SKU");
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = mixedTemplate(parent, rice, roti, naan);
        template.getSlots().get(1).getOptions().add(option(template.getSlots().get(1), rice, 5.0, 2, 0));

        when(configuredMenuTemplateRepository.findById(702L)).thenReturn(Optional.of(template));

        ConfiguredOrderInitiateRequest request = new ConfiguredOrderInitiateRequest();
        request.setOrderItems(List.of(new ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest(
                702L,
                1,
                List.of(
                        new ConfiguredOrderInitiateRequest.SlotItemRequest(100L, List.of(
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(30L, 1)
                        )),
                        new ConfiguredOrderInitiateRequest.SlotItemRequest(101L, List.of(
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(20L, 1),
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(21L, 2),
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(30L, 1)
                        ))
                )
        )));

        assertThatThrownBy(() -> configuredOrderService.initiateOrder(request))
                .hasMessageContaining("allows at most 2 selections");

        verify(orderRepository, never()).save(any(Order.class));
    }

    private ConfiguredOrderInitiateRequest request() {
        ConfiguredOrderInitiateRequest request = new ConfiguredOrderInitiateRequest();
        request.setOrderItems(List.of(new ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest(
                700L,
                1,
                List.of(new ConfiguredOrderInitiateRequest.SlotItemRequest(100L, List.of(
                        new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(20L, 3),
                        new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(21L, 1)
                )))
        )));
        return request;
    }

    private ConfiguredOrderInitiateRequest mixedRequest() {
        ConfiguredOrderInitiateRequest request = new ConfiguredOrderInitiateRequest();
        request.setOrderItems(List.of(new ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest(
                702L,
                1,
                List.of(
                        new ConfiguredOrderInitiateRequest.SlotItemRequest(100L, List.of(
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(30L, 1)
                        )),
                        new ConfiguredOrderInitiateRequest.SlotItemRequest(101L, List.of(
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(20L, 2),
                                new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(21L, 2)
                        ))
                )
        )));
        return request;
    }

    private ConfiguredMenuTemplate quantityTemplate(MenuItem parent, MenuItem roti, MenuItem naan) {
        ConfiguredMenuTemplate template = new ConfiguredMenuTemplate();
        template.setId(700L);
        template.setParentMenuItem(parent);
        template.setRestaurantId(parent.getRestaurantId());
        template.setIsActive(true);
        template.setIsDeleted(false);

        ConfiguredMenuSlot slot = new ConfiguredMenuSlot();
        slot.setId(100L);
        slot.setTemplate(template);
        slot.setSlotKey("breads");
        slot.setSlotName("Breads");
        slot.setMinSelections(1);
        slot.setMaxSelections(2);
        slot.setDisplayOrder(0);
        slot.setIsRequired(true);
        slot.getOptions().add(option(slot, roti, 10.0, 0, 2));
        slot.getOptions().add(option(slot, naan, 10.0, 1, 0));
        template.getSlots().add(slot);
        return template;
    }

    private ConfiguredMenuTemplate mixedTemplate(MenuItem parent, MenuItem rice, MenuItem roti, MenuItem naan) {
        ConfiguredMenuTemplate template = new ConfiguredMenuTemplate();
        template.setId(702L);
        template.setParentMenuItem(parent);
        template.setRestaurantId(parent.getRestaurantId());
        template.setIsActive(true);
        template.setIsDeleted(false);

        ConfiguredMenuSlot riceSlot = new ConfiguredMenuSlot();
        riceSlot.setId(100L);
        riceSlot.setTemplate(template);
        riceSlot.setSlotKey("rice");
        riceSlot.setSlotName("Rice");
        riceSlot.setMinSelections(1);
        riceSlot.setMaxSelections(1);
        riceSlot.setDisplayOrder(0);
        riceSlot.setIsRequired(true);
        riceSlot.getOptions().add(option(riceSlot, rice, 5.0, 0, 1));

        ConfiguredMenuSlot breadsSlot = new ConfiguredMenuSlot();
        breadsSlot.setId(101L);
        breadsSlot.setTemplate(template);
        breadsSlot.setSlotKey("breads");
        breadsSlot.setSlotName("Breads");
        breadsSlot.setMinSelections(1);
        breadsSlot.setMaxSelections(2);
        breadsSlot.setDisplayOrder(1);
        breadsSlot.setIsRequired(true);
        breadsSlot.getOptions().add(option(breadsSlot, roti, 5.0, 0, 1));
        breadsSlot.getOptions().add(option(breadsSlot, naan, 10.0, 1, 1));

        template.getSlots().add(riceSlot);
        template.getSlots().add(breadsSlot);
        return template;
    }

    private ConfiguredMenuOption option(ConfiguredMenuSlot slot,
                                        MenuItem child,
                                        Double delta,
                                        Integer displayOrder,
                                        Integer minQuantity) {
        ConfiguredMenuOption option = new ConfiguredMenuOption();
        option.setSlot(slot);
        option.setChildMenuItem(child);
        option.setPriceDelta(delta);
        option.setDisplayOrder(displayOrder);
        option.setIsDefault(false);
        option.setMinQuantity(minQuantity);
        return option;
    }

    private MenuItem menuItem(Long id, Long restaurantId, String name, MenuType type, Double price, String sku) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setItemName(name);
        menuItem.setMenuType(type);
        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(price);
        menuItem.setItemPrice(itemPrice);
        if (sku != null) {
            ItemStock itemStock = new ItemStock();
            itemStock.setSku(sku);
            itemStock.setMenuItem(menuItem);
            itemStock.setRestaurantId(restaurantId);
            itemStock.setTotalStock(100);
            itemStock.setReorderLevel(0);
            itemStock.setUnitOfMeasure("pcs");
            itemStock.setIsActive(true);
            itemStock.setIsDeleted(false);
            menuItem.setItemStock(itemStock);
        }
        return menuItem;
    }
}
