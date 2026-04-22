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
import com.kritik.POS.order.model.request.OrderV2InitiateRequest;
import com.kritik.POS.order.model.response.OrderV2Response;
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
class OrderV2ServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ConfiguredSaleItemRepository configuredSaleItemRepository;

    @Mock
    private MenuCatalogApi menuCatalogApi;

    @Mock
    private ConfiguredMenuApi configuredMenuApi;

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
    private OrderV2ServiceImpl orderV2Service;

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
    void initiateOrderAppliesPriceDeltaOnlyAboveMinimumQuantityAndScalesStock() {
        List<ConfiguredSaleItem> persistedConfiguredItems = new ArrayList<>();
        stubConfiguredPersistence(persistedConfiguredItems, 900L);

        when(menuCatalogApi.getAccessibleMenuItem(10L)).thenReturn(configurableMenu(10L, "Thali", 200.00));
        when(menuCatalogApi.getAccessibleMenuItem(20L)).thenReturn(directMenu(20L, "Roti", "ROTI-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(21L)).thenReturn(directMenu(21L, "Naan", "NAAN-SKU"));
        when(configuredMenuApi.getAccessibleActiveTemplateByParentMenuItemId(10L)).thenReturn(quantityTemplate(10L, 200.00, 20L, "Roti", 10.00, 0, 21L, "Naan", 20.00, 1));

        OrderV2Response response = orderV2Service.initiateOrder(orderRequestWithSingleSlot(10L, 2, List.of(
                new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 2),
                new OrderV2InitiateRequest.SlotItemQuantityRequest(21L, 2)
        )));

        assertThat(response.getConfiguredItems()).hasSize(1);
        assertThat(response.getConfiguredItems().get(0).unitPrice()).isEqualByComparingTo("230.00");
        assertThat(response.getConfiguredItems().get(0).lineTotal()).isEqualByComparingTo("460.00");
        assertThat(response.getConfiguredItems().get(0).items())
                .extracting(item -> item.childItemName(), item -> item.quantity())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Roti", 2),
                        org.assertj.core.groups.Tuple.tuple("Naan", 2)
                );

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryApi).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 4),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 4)
                );
    }

    @Test
    void initiateOrderRejectsQuantityBelowConfiguredMinimum() {
        when(menuCatalogApi.getAccessibleMenuItem(10L)).thenReturn(configurableMenu(10L, "Thali", 200.00));
        when(configuredMenuApi.getAccessibleActiveTemplateByParentMenuItemId(10L)).thenReturn(quantityTemplate(10L, 200.00, 20L, "Roti", 10.00, 2, 21L, "Naan", 20.00, 1));

        assertThatThrownBy(() -> orderV2Service.initiateOrder(orderRequestWithSingleSlot(10L, 1, List.of(
                new OrderV2InitiateRequest.SlotItemQuantityRequest(20L, 1)
        ))))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Roti requires at least 2 quantity");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void initiateOrderSupportsExactOneAndMixableSelectionSlotsInSameCartLine() {
        List<ConfiguredSaleItem> persistedConfiguredItems = new ArrayList<>();
        stubConfiguredPersistence(persistedConfiguredItems, 901L);

        when(menuCatalogApi.getAccessibleMenuItem(10L)).thenReturn(configurableMenu(10L, "Executive Thali", 200.00));
        when(menuCatalogApi.getAccessibleMenuItem(30L)).thenReturn(directMenu(30L, "Jeera Rice", "RICE-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(20L)).thenReturn(directMenu(20L, "Roti", "ROTI-SKU"));
        when(menuCatalogApi.getAccessibleMenuItem(21L)).thenReturn(directMenu(21L, "Naan", "NAAN-SKU"));
        when(configuredMenuApi.getAccessibleActiveTemplateByParentMenuItemId(10L)).thenReturn(mixedTemplate());

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
        assertThat(response.getConfiguredItems().get(0).unitPrice()).isEqualByComparingTo("230.00");
        assertThat(response.getConfiguredItems().get(0).lineTotal()).isEqualByComparingTo("460.00");
        assertThat(response.getConfiguredItems().get(0).items())
                .extracting(item -> item.childItemName(), item -> item.quantity())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Jeera Rice", 1),
                        org.assertj.core.groups.Tuple.tuple("Roti", 2),
                        org.assertj.core.groups.Tuple.tuple("Naan", 2)
                );

        ArgumentCaptor<List<StockRequest>> stockCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryApi).checkOrderStockAvailability(stockCaptor.capture(), any(), any());
        assertThat(stockCaptor.getValue())
                .extracting(StockRequest::sku, StockRequest::amount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("RICE-SKU", 2),
                        org.assertj.core.groups.Tuple.tuple("ROTI-SKU", 4),
                        org.assertj.core.groups.Tuple.tuple("NAAN-SKU", 4)
                );
    }

    @Test
    void initiateOrderRejectsDuplicateSlotItems() {
        when(menuCatalogApi.getAccessibleMenuItem(10L)).thenReturn(configurableMenu(10L, "Executive Thali", 200.00));
        when(configuredMenuApi.getAccessibleActiveTemplateByParentMenuItemId(10L)).thenReturn(mixedTemplate());

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

    private void stubConfiguredPersistence(List<ConfiguredSaleItem> persistedConfiguredItems, Long orderId) {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(orderId);
            return order;
        });
        when(configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(orderId)).thenAnswer(invocation -> persistedConfiguredItems);
        when(configuredSaleItemRepository.saveAll(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<ConfiguredSaleItem> items = invocation.getArgument(0);
            persistedConfiguredItems.clear();
            persistedConfiguredItems.addAll(items);
            return items;
        });
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

    private ConfiguredMenuTemplateSnapshot quantityTemplate(Long parentId,
                                                            double basePrice,
                                                            Long childAId,
                                                            String childAName,
                                                            double childADelta,
                                                            int childAMin,
                                                            Long childBId,
                                                            String childBName,
                                                            double childBDelta,
                                                            int childBMin) {
        return new ConfiguredMenuTemplateSnapshot(
                700L,
                101L,
                true,
                configurableMenu(parentId, "Thali", basePrice),
                List.of(new ConfiguredMenuSlotSnapshot(
                        100L,
                        "breads",
                        "Breads",
                        1,
                        2,
                        true,
                        List.of(
                                new ConfiguredMenuOptionSnapshot(1L, childAId, childAName, money(childADelta), childAMin),
                                new ConfiguredMenuOptionSnapshot(2L, childBId, childBName, money(childBDelta), childBMin)
                        )
                ))
        );
    }

    private ConfiguredMenuTemplateSnapshot mixedTemplate() {
        return new ConfiguredMenuTemplateSnapshot(
                702L,
                101L,
                true,
                configurableMenu(10L, "Executive Thali", 200.00),
                List.of(
                        new ConfiguredMenuSlotSnapshot(
                                100L,
                                "rice",
                                "Rice",
                                1,
                                1,
                                true,
                                List.of(new ConfiguredMenuOptionSnapshot(1L, 30L, "Jeera Rice", money(15.00), 1))
                        ),
                        new ConfiguredMenuSlotSnapshot(
                                101L,
                                "breads",
                                "Breads",
                                1,
                                2,
                                true,
                                List.of(
                                        new ConfiguredMenuOptionSnapshot(2L, 20L, "Roti", money(10.00), 1),
                                        new ConfiguredMenuOptionSnapshot(3L, 21L, "Naan", money(20.00), 1)
                                )
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
