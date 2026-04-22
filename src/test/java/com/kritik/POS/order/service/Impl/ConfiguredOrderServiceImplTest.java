package com.kritik.POS.order.service.Impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kritik.POS.common.util.MoneyUtils;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuApi;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuOptionSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuSlotSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuTemplateSnapshot;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.api.InventoryApi;
import com.kritik.POS.inventory.api.StockRequest;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.restaurant.api.MenuItemType;
import com.kritik.POS.restaurant.api.MenuPriceSnapshot;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.api.TaxApi;
import com.kritik.POS.tax.api.TaxClassSnapshot;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ConfiguredOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ConfiguredSaleItemRepository configuredSaleItemRepository;

    @Mock
    private ConfiguredMenuApi configuredMenuApi;

    @Mock
    private MenuCatalogApi menuCatalogApi;

    @Mock
    private InventoryApi inventoryApi;

    @Mock
    private TaxApi taxApi;

    @Mock
    private OrderPricingService orderPricingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private ConfiguredOrderServiceImpl configuredOrderService;

    @BeforeEach
    void setUp() {
        when(taxApi.resolveTaxClass(any(), any())).thenReturn(new TaxClassSnapshot(1L, 101L, "GST", false));
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<OrderPricingService.LinePricingPlan> plans = invocation.getArgument(2);
            BigDecimal total = BigDecimal.ZERO;

            for (OrderPricingService.LinePricingPlan plan : plans) {
                BigDecimal lineTotal = plan.lineSubtotalAmount().subtract(plan.lineDiscountAmount());
                total = total.add(lineTotal);
                if (plan.configuredSaleItem() != null) {
                    ConfiguredSaleItem item = plan.configuredSaleItem();
                    item.setTaxClassCodeSnapshot(plan.taxClassCodeSnapshot());
                    item.setPriceIncludesTax(plan.priceIncludesTax());
                    item.setUnitListAmount(plan.unitListAmount());
                    item.setUnitDiscountAmount(plan.unitDiscountAmount());
                    item.setUnitTaxableAmount(item.getUnitPrice());
                    item.setUnitTaxAmount(BigDecimal.ZERO);
                    item.setUnitTotalAmount(item.getUnitPrice());
                    item.setLineSubtotalAmount(plan.lineSubtotalAmount());
                    item.setLineDiscountAmount(plan.lineDiscountAmount());
                    item.setLineTaxableAmount(lineTotal);
                    item.setLineTaxAmount(BigDecimal.ZERO);
                    item.setLineTotalAmount(lineTotal);
                }
            }

            order.setSubtotalAmount(total);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setTaxableAmount(total);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setFeeAmount(BigDecimal.ZERO);
            order.setRoundingAmount(BigDecimal.ZERO);
            order.setGrandTotal(total);
            order.setTotalPrice(total);
            return null;
        }).when(orderPricingService).applyPricing(any(Order.class), any(), anyList(), any());
    }

    @Test
    void initiateOrderAppliesPriceDeltaOnlyAboveMinimumQuantityForConfiguredOrders() {
        List<ConfiguredSaleItem> persistedItems = new ArrayList<>();
        stubConfiguredPersistence(persistedItems, 901L);

        when(configuredMenuApi.getAccessibleActiveTemplate(700L)).thenReturn(quantityTemplate());
        when(menuCatalogApi.getAccessibleMenuItem(20L)).thenReturn(directMenu(20L, "Roti", "ROTI-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(21L)).thenReturn(directMenu(21L, "Naan", "NAAN-SKU"));

        ConfiguredOrderResponse response = configuredOrderService.initiateOrder(request());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).unitPrice()).isEqualByComparingTo("200.00");
        assertThat(response.getItems().get(0).lineTotal()).isEqualByComparingTo("200.00");
        assertThat(response.getItems().get(0).items())
                .extracting(item -> item.quantity())
                .containsExactlyInAnyOrder(3, 1);

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryApi).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 3),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 1)
                );
    }

    @Test
    void initiateOrderSupportsExactOneAndMixableSelectionSlotsForConfiguredOrders() {
        List<ConfiguredSaleItem> persistedItems = new ArrayList<>();
        stubConfiguredPersistence(persistedItems, 902L);

        when(configuredMenuApi.getAccessibleActiveTemplate(702L)).thenReturn(mixedTemplate());
        when(menuCatalogApi.getAccessibleMenuItem(30L)).thenReturn(directMenu(30L, "Rice", "RICE-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(20L)).thenReturn(directMenu(20L, "Roti", "ROTI-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(21L)).thenReturn(directMenu(21L, "Naan", "NAAN-SKU"));

        ConfiguredOrderResponse response = configuredOrderService.initiateOrder(mixedRequest());

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).unitPrice()).isEqualByComparingTo("195.00");
        assertThat(response.getItems().get(0).items())
                .extracting(item -> item.childItemName(), item -> item.quantity())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Rice", 1),
                        org.assertj.core.groups.Tuple.tuple("Roti", 2),
                        org.assertj.core.groups.Tuple.tuple("Naan", 2)
                );

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryApi).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
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
        when(configuredMenuApi.getAccessibleActiveTemplate(700L)).thenReturn(quantityTemplate());

        ConfiguredOrderInitiateRequest request = new ConfiguredOrderInitiateRequest();
        request.setOrderItems(List.of(new ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest(
                700L,
                1,
                List.of(new ConfiguredOrderInitiateRequest.SlotItemRequest(
                        100L,
                        List.of(new ConfiguredOrderInitiateRequest.SlotItemQuantityRequest(20L, 1))
                ))
        )));

        assertThatThrownBy(() -> configuredOrderService.initiateOrder(request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Roti requires at least 2 quantity");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderRejectsTooManySelectedItemsForSlot() {
        when(configuredMenuApi.getAccessibleActiveTemplate(702L)).thenReturn(mixedTemplateWithExtraBreadChoice());

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

    private void stubConfiguredPersistence(List<ConfiguredSaleItem> persistedItems, Long orderId) {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedItems.clear();
            persistedItems.addAll(items);
            return items;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(orderId)).thenAnswer(invocation -> persistedItems);
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

    private ConfiguredMenuTemplateSnapshot quantityTemplate() {
        return new ConfiguredMenuTemplateSnapshot(
                700L,
                101L,
                true,
                configurableMenu(10L, "Thali", 180.00),
                List.of(new ConfiguredMenuSlotSnapshot(
                        100L,
                        "breads",
                        "Breads",
                        1,
                        2,
                        true,
                        List.of(
                                new ConfiguredMenuOptionSnapshot(1L, 20L, "Roti", money(10.00), 2),
                                new ConfiguredMenuOptionSnapshot(2L, 21L, "Naan", money(10.00), 0)
                        )
                ))
        );
    }

    private ConfiguredMenuTemplateSnapshot mixedTemplate() {
        return new ConfiguredMenuTemplateSnapshot(
                702L,
                101L,
                true,
                configurableMenu(10L, "Thali", 180.00),
                List.of(
                        new ConfiguredMenuSlotSnapshot(
                                100L,
                                "rice",
                                "Rice",
                                1,
                                1,
                                true,
                                List.of(new ConfiguredMenuOptionSnapshot(1L, 30L, "Rice", money(5.00), 1))
                        ),
                        new ConfiguredMenuSlotSnapshot(
                                101L,
                                "breads",
                                "Breads",
                                1,
                                2,
                                true,
                                List.of(
                                        new ConfiguredMenuOptionSnapshot(2L, 20L, "Roti", money(5.00), 1),
                                        new ConfiguredMenuOptionSnapshot(3L, 21L, "Naan", money(10.00), 1)
                                )
                        )
                )
        );
    }

    private ConfiguredMenuTemplateSnapshot mixedTemplateWithExtraBreadChoice() {
        ConfiguredMenuTemplateSnapshot base = mixedTemplate();
        List<ConfiguredMenuOptionSnapshot> extraBreadOptions = new ArrayList<>(base.slots().get(1).options());
        extraBreadOptions.add(new ConfiguredMenuOptionSnapshot(4L, 30L, "Rice", money(5.00), 0));
        return new ConfiguredMenuTemplateSnapshot(
                base.id(),
                base.restaurantId(),
                base.active(),
                base.parentMenuItem(),
                List.of(
                        base.slots().get(0),
                        new ConfiguredMenuSlotSnapshot(
                                101L,
                                "breads",
                                "Breads",
                                1,
                                2,
                                true,
                                extraBreadOptions
                        )
                )
        );
    }

    private MenuItemSnapshot configurableMenu(Long id, String name, double price) {
        return new MenuItemSnapshot(
                id,
                101L,
                1L,
                name,
                name,
                true,
                false,
                true,
                MenuItemType.CONFIGURABLE,
                new MenuPriceSnapshot(money(price), money(price), BigDecimal.ZERO, false),
                null,
                List.of()
        );
    }

    private MenuItemSnapshot directMenu(Long id, String name, String sku) {
        return new MenuItemSnapshot(
                id,
                101L,
                1L,
                name,
                name,
                true,
                false,
                true,
                MenuItemType.DIRECT,
                new MenuPriceSnapshot(BigDecimal.ZERO.setScale(MoneyUtils.MONEY_SCALE), BigDecimal.ZERO.setScale(MoneyUtils.MONEY_SCALE), BigDecimal.ZERO, false),
                sku,
                List.of()
        );
    }

    private BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(MoneyUtils.MONEY_SCALE, java.math.RoundingMode.HALF_UP);
    }
}
