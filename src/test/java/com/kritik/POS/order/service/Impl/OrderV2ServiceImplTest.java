package com.kritik.POS.order.service.Impl;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.entity.stock.ItemStock;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.request.OrderV2InitiateRequest;
import com.kritik.POS.order.model.response.OrderV2Response;
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
class OrderV2ServiceImplTest {

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
    private OrderV2ServiceImpl orderV2Service;

    @Test
    void initiateOrderAppliesPriceDeltaOnlyAboveMinimumQuantityAndScalesStock() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 200.0, null);
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = quantityTemplate(parent, roti, naan);

        List<ConfiguredSaleItem> persistedConfiguredItems = new ArrayList<>();
        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(900L);
            return order;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(900L)).thenAnswer(invocation -> persistedConfiguredItems);
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedConfiguredItems.clear();
            persistedConfiguredItems.addAll(items);
            return items;
        });

        OrderV2Response response = orderV2Service.initiateOrder(orderRequestWithSingleSlot(10L, 2, List.of(
                new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 2),
                new OrderV2InitiateRequest.SlotItemQuantityRequest(21L, 2)
        )));

        assertThat(response.getConfiguredItems()).hasSize(1);
        assertThat(response.getConfiguredItems().get(0).unitPrice()).isEqualTo(230.0);
        assertThat(response.getConfiguredItems().get(0).lineTotal()).isEqualTo(460.0);
        assertThat(response.getConfiguredItems().get(0).items()).hasSize(2);
        assertThat(response.getConfiguredItems().get(0).items().get(0).quantity()).isEqualTo(2);
        assertThat(response.getConfiguredItems().get(0).items().get(1).quantity()).isEqualTo(2);

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 4),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 4)
                );
    }

    @Test
    void initiateOrderRejectsQuantityBelowConfiguredMinimum() {
        MenuItem parent = menuItem(10L, 101L, "Thali", MenuType.CONFIGURABLE, 200.0, null);
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = quantityTemplate(parent, roti, naan);

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> orderV2Service.initiateOrder(orderRequestWithSingleSlot(10L, 1, List.of(
                new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 1)
        ))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Roti requires at least 2 quantity");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderRejectsMoreThanOneSelectedItemForExactOneSlot() {
        MenuItem parent = menuItem(10L, 101L, "Mini Thali", MenuType.CONFIGURABLE, 150.0, null);
        MenuItem paneer = menuItem(30L, 101L, "Paneer", MenuType.DIRECT, 0.0, "PANEER-SKU");
        MenuItem dal = menuItem(31L, 101L, "Dal", MenuType.DIRECT, 0.0, "DAL-SKU");
        ConfiguredMenuTemplate template = exactOneTemplate(parent, paneer);
        template.getSlots().get(0).getOptions().add(option(template.getSlots().get(0), dal, 0.0, 1, 0));

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> orderV2Service.initiateOrder(orderRequestWithSingleSlot(10L, 1, List.of(
                new OrderV2InitiateRequest.SlotItemQuantityRequest(30L, 1),
                new OrderV2InitiateRequest.SlotItemQuantityRequest(31L, 1)
        ))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("allows at most 1 selections");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderSupportsExactOneAndMixableSelectionSlotsInSameCartLine() {
        MenuItem parent = menuItem(10L, 101L, "Executive Thali", MenuType.CONFIGURABLE, 200.0, null);
        MenuItem jeeraRice = menuItem(30L, 101L, "Jeera Rice", MenuType.DIRECT, 0.0, "RICE-SKU");
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = mixedTemplate(parent, jeeraRice, roti, naan);

        List<ConfiguredSaleItem> persistedConfiguredItems = new ArrayList<>();
        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(inventoryService.getAccessibleMenuItem(30L)).thenReturn(jeeraRice);
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));
        when(taxService.getActiveTaxRates()).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(901L);
            return order;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(901L)).thenAnswer(invocation -> persistedConfiguredItems);
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedConfiguredItems.clear();
            persistedConfiguredItems.addAll(items);
            return items;
        });

        OrderV2Response response = orderV2Service.initiateOrder(orderRequest(10L, 2, List.of(
                new OrderV2InitiateRequest.SlotItemRequest(100L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(30L, 1)
                )),
                new OrderV2InitiateRequest.SlotItemRequest(101L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 2),
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(21L, 2)
                ))
        )));

        assertThat(response.getConfiguredItems()).hasSize(1);
        assertThat(response.getConfiguredItems().get(0).unitPrice()).isEqualTo(230.0);
        assertThat(response.getConfiguredItems().get(0).lineTotal()).isEqualTo(460.0);
        assertThat(response.getConfiguredItems().get(0).items())
                .extracting(item -> item.childItemName(), item -> item.quantity())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Jeera Rice", 1),
                        org.assertj.core.groups.Tuple.tuple("Roti", 2),
                        org.assertj.core.groups.Tuple.tuple("Naan", 2)
                );

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("RICE-SKU", 2),
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 4),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 4)
                );
    }

    @Test
    void initiateOrderRejectsTooManySelectedItemsForMixableSlot() {
        MenuItem parent = menuItem(10L, 101L, "Executive Thali", MenuType.CONFIGURABLE, 200.0, null);
        MenuItem plainRice = menuItem(30L, 101L, "Plain Rice", MenuType.DIRECT, 0.0, "RICE-SKU");
        MenuItem jeeraRice = menuItem(31L, 101L, "Jeera Rice", MenuType.DIRECT, 0.0, "RICE2-SKU");
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = mixedTemplateWithTwoRiceChoices(parent, plainRice, jeeraRice, roti, naan);
        template.getSlots().get(1).getOptions().add(option(template.getSlots().get(1), plainRice, 5.0, 2, 0));

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> orderV2Service.initiateOrder(orderRequest(10L, 1, List.of(
                new OrderV2InitiateRequest.SlotItemRequest(100L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(30L, 1),
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(31L, 1)
                )),
                new OrderV2InitiateRequest.SlotItemRequest(101L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 2),
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(21L, 2),
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(30L, 1)
                ))
        ))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("allows at most 2 selections");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderRejectsDuplicateSlotItems() {
        MenuItem parent = menuItem(10L, 101L, "Executive Thali", MenuType.CONFIGURABLE, 200.0, null);
        MenuItem jeeraRice = menuItem(30L, 101L, "Jeera Rice", MenuType.DIRECT, 0.0, "RICE-SKU");
        MenuItem roti = menuItem(20L, 101L, "Roti", MenuType.DIRECT, 0.0, "ROTI-SKU");
        MenuItem naan = menuItem(21L, 101L, "Naan", MenuType.DIRECT, 0.0, "NAAN-SKU");
        ConfiguredMenuTemplate template = mixedTemplate(parent, jeeraRice, roti, naan);

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parent);
        when(configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> orderV2Service.initiateOrder(orderRequest(10L, 1, List.of(
                new OrderV2InitiateRequest.SlotItemRequest(100L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(30L, 1)
                )),
                new OrderV2InitiateRequest.SlotItemRequest(101L, List.of(
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 1),
                        new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 3)
                ))
        ))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Duplicate slot item");

        verify(orderRepository, never()).save(any(Order.class));
    }

    private OrderV2InitiateRequest orderRequestWithSingleSlot(Long parentMenuItemId,
                                                              Integer amount,
                                                              List<OrderV2InitiateRequest.SlotItemQuantityRequest> slotItems) {
        return orderRequest(parentMenuItemId, amount, List.of(
                new OrderV2InitiateRequest.SlotItemRequest(100L, slotItems)
        ));
    }

    private OrderV2InitiateRequest orderRequest(Long parentMenuItemId,
                                                Integer amount,
                                                List<OrderV2InitiateRequest.SlotItemRequest> slotItems) {
        OrderV2InitiateRequest request = new OrderV2InitiateRequest();
        request.setOrderItems(List.of(new OrderV2InitiateRequest.OrderItemRequest(
                parentMenuItemId,
                amount,
                new OrderV2InitiateRequest.ConfigurationRequest(slotItems)
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
        slot.getOptions().add(option(slot, roti, 10.0, 0, 1));
        slot.getOptions().add(option(slot, naan, 20.0, 1, 1));
        template.getSlots().add(slot);
        return template;
    }

    private ConfiguredMenuTemplate exactOneTemplate(MenuItem parent, MenuItem paneer) {
        ConfiguredMenuTemplate template = new ConfiguredMenuTemplate();
        template.setId(701L);
        template.setParentMenuItem(parent);
        template.setRestaurantId(parent.getRestaurantId());
        template.setIsActive(true);
        template.setIsDeleted(false);

        ConfiguredMenuSlot slot = new ConfiguredMenuSlot();
        slot.setId(100L);
        slot.setTemplate(template);
        slot.setSlotKey("main");
        slot.setSlotName("Main Curry");
        slot.setMinSelections(1);
        slot.setMaxSelections(1);
        slot.setDisplayOrder(0);
        slot.setIsRequired(true);
        slot.getOptions().add(option(slot, paneer, 0.0, 0, 1));
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
        riceSlot.getOptions().add(option(riceSlot, rice, 15.0, 0, 1));

        ConfiguredMenuSlot breadsSlot = new ConfiguredMenuSlot();
        breadsSlot.setId(101L);
        breadsSlot.setTemplate(template);
        breadsSlot.setSlotKey("breads");
        breadsSlot.setSlotName("Breads");
        breadsSlot.setMinSelections(1);
        breadsSlot.setMaxSelections(2);
        breadsSlot.setDisplayOrder(1);
        breadsSlot.setIsRequired(true);
        breadsSlot.getOptions().add(option(breadsSlot, roti, 10.0, 0, 1));
        breadsSlot.getOptions().add(option(breadsSlot, naan, 20.0, 1, 1));

        template.getSlots().add(riceSlot);
        template.getSlots().add(breadsSlot);
        return template;
    }

    private ConfiguredMenuTemplate mixedTemplateWithTwoRiceChoices(MenuItem parent,
                                                                   MenuItem plainRice,
                                                                   MenuItem jeeraRice,
                                                                   MenuItem roti,
                                                                   MenuItem naan) {
        ConfiguredMenuTemplate template = mixedTemplate(parent, plainRice, roti, naan);
        template.getSlots().get(0).getOptions().add(option(template.getSlots().get(0), jeeraRice, 15.0, 1, 1));
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
