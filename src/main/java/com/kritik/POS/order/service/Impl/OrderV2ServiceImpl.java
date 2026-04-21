package com.kritik.POS.order.service.Impl;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.events.OrderCompletedEvent;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.inventory.util.InventoryAvailabilityUtil;
import com.kritik.POS.inventory.util.InventoryUtil;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.ConfiguredSaleItemSelection;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.OrderV2InitiateRequest;
import com.kritik.POS.order.model.request.OrderV2UpdateRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.ConfiguredSaleItemResponse;
import com.kritik.POS.order.model.response.OrderSaleItemResponse;
import com.kritik.POS.order.model.response.OrderV2Response;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.order.service.OrderV2Service;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.models.request.StockRequest;
import com.kritik.POS.restaurant.util.RestaurantUtil;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import com.kritik.POS.tax.service.TaxService;
import com.kritik.POS.tax.util.MoneyUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderV2ServiceImpl implements OrderV2Service {

    private final OrderRepository orderRepository;
    private final ConfiguredSaleItemRepository configuredSaleItemRepository;
    private final ConfiguredMenuTemplateRepository configuredMenuTemplateRepository;
    private final InventoryService inventoryService;
    private final TaxService taxService;
    private final OrderPricingService orderPricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public OrderV2Response initiateOrder(OrderV2InitiateRequest request) {
        Order order = new Order();
        OrderV2Draft draft = buildOrderDraft(order, request.getOrderItems(), request.getTaxContext());
        applyOrderDraft(order, draft);
        order.setPaymentType(resolvePaymentType(request.getPaymentType(), PaymentType.CASH));
        order.setPaymentInitiatedTime(LocalDateTime.now());
        order.setOrderId(UUID.randomUUID().toString());

        Order savedOrder = orderRepository.save(order);
        persistConfiguredSaleItems(savedOrder, draft.configuredItems());
        return buildResponse(
                savedOrder,
                "Payment Initiation Complete",
                "Your order is ready for payment. Please select your preferred payment method to continue."
        );
    }

    @Override
    @Transactional
    public OrderV2Response updateOrder(String orderId, OrderV2UpdateRequest request) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cart can only be updated before payment confirmation", HttpStatus.BAD_REQUEST);
        }

        OrderV2Draft draft = buildOrderDraft(order, request.getOrderItems(), request.getTaxContext());
        applyOrderDraft(order, draft);
        order.setPaymentType(resolvePaymentType(request.getPaymentType(), order.getPaymentType()));
        Order savedOrder = orderRepository.save(order);
        replaceConfiguredSaleItems(savedOrder, draft.configuredItems());
        return buildResponse(
                savedOrder,
                "Order Updated",
                "Your cart has been updated while payment is still pending confirmation."
        );
    }

    @Override
    @Transactional
    public OrderV2Response cancelTransaction(String orderId) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cant cancel at this moment");
        }
        order.setPaymentStatus(PaymentStatus.PAYMENT_CANCELED);
        order.setCancelledAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        return buildResponse(
                savedOrder,
                "Payment Canceled",
                "Your payment has been canceled. You can start adding items again when you're ready."
        );
    }

    @Override
    @Transactional
    public OrderV2Response completePayment(String orderId, CompletePaymentRequest request) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            return buildResponse(
                    order,
                    "Payment Already Completed",
                    "This order was already marked as paid. Returning the existing payment result."
            );
        }
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Payment cannot be completed in the current state");
        }

        StockRequirements requirements = buildPersistedStockRequirements(order);
        inventoryService.checkOrderStockAvailability(
                requirements.stockRequests(),
                requirements.ingredientRequirements(),
                requirements.preparedRequirements()
        );
        inventoryService.deductStockForRequirements(
                requirements.stockRequests(),
                requirements.ingredientRequirements(),
                requirements.preparedRequirements(),
                requirements.affectedMenuIds()
        );

        PaymentType paymentType = request == null ? null : request.getPaymentType();
        order.setPaymentStatus(PaymentStatus.PAYMENT_SUCCESSFUL);
        order.setPaymentType(resolvePaymentType(paymentType, order.getPaymentType()));
        order.setPaymentCompletedAt(LocalDateTime.now());
        applyPaymentAudit(order, request);
        Order savedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getOrderId()));
        return buildResponse(
                savedOrder,
                "Thank You!",
                "Your transaction is complete. Thank you for your purchase!"
        );
    }

    @Override
    @Transactional
    public OrderV2Response refundPayment(String orderId, RefundPaymentRequest request) {
        Order order = getAccessibleOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_REFUND)) {
            return buildResponse(
                    order,
                    "Order Already Refunded",
                    "This order has already been refunded. Returning the current refund state."
            );
        }
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            throw new OrderException("Payment has not been completed yet");
        }

        LocalDateTime paymentTime = order.getPaymentCompletedAt() != null
                ? order.getPaymentCompletedAt()
                : order.getPaymentInitiatedTime();
        if (paymentTime != null && Duration.between(paymentTime, LocalDateTime.now()).toHours() > 24) {
            throw new OrderException("Can't complete the refund");
        }

        StockRequirements requirements = buildPersistedStockRequirements(order);
        inventoryService.restoreStockForRequirements(
                requirements.stockRequests(),
                requirements.ingredientRequirements(),
                requirements.preparedRequirements(),
                requirements.affectedMenuIds()
        );
        order.setPaymentStatus(PaymentStatus.PAYMENT_REFUND);
        order.setRefundedAt(LocalDateTime.now());
        applyRefundAudit(order, request);
        Order savedOrder = orderRepository.save(order);
        return buildResponse(
                savedOrder,
                "Order Refunded",
                "Order refunded successfully and stock has been restored."
        );
    }

    private OrderV2Draft buildOrderDraft(Order order,
                                         List<OrderV2InitiateRequest.OrderItemRequest> orderItems,
                                         com.kritik.POS.order.model.request.OrderTaxContextRequest taxContextRequest) {
        List<SaleItem> saleItems = new ArrayList<>();
        List<ConfiguredSaleItem> configuredItems = new ArrayList<>();
        List<StockRequest> stockRequests = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        Set<Long> affectedMenuIds = new LinkedHashSet<>();
        List<OrderPricingService.LinePricingPlan> linePlans = new ArrayList<>();
        Long orderRestaurantId = null;
        int lineIndex = 0;

        for (OrderV2InitiateRequest.OrderItemRequest orderItemRequest : orderItems) {
            MenuItem menuItem = inventoryService.getAccessibleMenuItem(orderItemRequest.menuItemId());
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, menuItem.getRestaurantId());

            if (InventoryAvailabilityUtil.resolveMenuType(menuItem) == MenuType.CONFIGURABLE) {
                if (orderItemRequest.configuration() == null) {
                    throw new AppException("Configuration is required for configurable menu item " + menuItem.getItemName(), HttpStatus.BAD_REQUEST);
                }
                ConfiguredLineDraft configuredLineDraft = buildConfiguredLine(menuItem, orderItemRequest, "cfg-" + lineIndex++);
                configuredItems.add(configuredLineDraft.item());
                linePlans.add(configuredLineDraft.pricingPlan());
                mergeRequirements(stockRequests, ingredientRequirements, preparedRequirements, affectedMenuIds, configuredLineDraft.requirements());
                continue;
            }

            if (orderItemRequest.configuration() != null) {
                throw new AppException("Configuration is only allowed for configurable menu items", HttpStatus.BAD_REQUEST);
            }

            TaxClass taxClass = taxService.resolveTaxClass(menuItem.getRestaurantId(), menuItem.getTaxClassId());
            SaleItem saleItem = new SaleItem();
            saleItem.setAmount(orderItemRequest.amount());
            saleItem.setSaleItemName(menuItem.getItemName());
            saleItem.setMenuItem(menuItem);
            saleItem.setRestaurantId(menuItem.getRestaurantId());
            saleItem.setOrder(order);
            saleItems.add(saleItem);
            BigDecimal quantity = BigDecimal.valueOf(orderItemRequest.amount());
            BigDecimal unitListAmount = MoneyUtils.money(menuItem.getItemPrice().getPrice());
            BigDecimal unitDiscountedAmount = RestaurantUtil.getMenuItemPrice(menuItem.getItemPrice());
            BigDecimal unitDiscountAmount = MoneyUtils.subtract(unitListAmount, unitDiscountedAmount);
            BigDecimal lineSubtotalAmount = MoneyUtils.multiply(unitListAmount, quantity);
            BigDecimal lineDiscountAmount = MoneyUtils.multiply(unitDiscountAmount, quantity);
            String referenceKey = "sale-" + lineIndex++;
            linePlans.add(OrderPricingService.LinePricingPlan.forSaleItem(
                    referenceKey,
                    taxClass.getCode(),
                    Boolean.TRUE.equals(menuItem.getItemPrice().getPriceIncludesTax()),
                    unitListAmount,
                    unitDiscountAmount,
                    lineSubtotalAmount,
                    lineDiscountAmount,
                    List.of(new TaxableChargeComponent(
                            referenceKey,
                            taxClass.getCode(),
                            taxClass.getId(),
                            MoneyUtils.multiply(unitDiscountedAmount, quantity),
                            Boolean.TRUE.equals(menuItem.getItemPrice().getPriceIncludesTax())
                    )),
                    saleItem
            ));

            InventoryAvailabilityUtil.accumulateStockRequirements(
                    menuItem,
                    saleItem.getAmount(),
                    stockRequests,
                    ingredientRequirements,
                    preparedRequirements
            );
            affectedMenuIds.add(menuItem.getId());
        }

        inventoryService.checkOrderStockAvailability(stockRequests, ingredientRequirements, preparedRequirements);
        return new OrderV2Draft(
                orderRestaurantId,
                saleItems,
                configuredItems,
                linePlans,
                taxContextRequest
        );
    }

    private ConfiguredLineDraft buildConfiguredLine(MenuItem parentMenuItem,
                                                    OrderV2InitiateRequest.OrderItemRequest orderItemRequest,
                                                    String lineKey) {
        ConfiguredMenuTemplate template = configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(parentMenuItem.getId())
                .orElseThrow(() -> new AppException("Configured menu template not found for menu item", HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new AppException("Configured menu template is inactive", HttpStatus.BAD_REQUEST);
        }

        TaxClass parentTaxClass = taxService.resolveTaxClass(parentMenuItem.getRestaurantId(), parentMenuItem.getTaxClassId());
        BigDecimal basePrice = RestaurantUtil.getMenuItemPrice(parentMenuItem.getItemPrice());
        BigDecimal optionDeltaTotal = MoneyUtils.zero();
        ConfiguredSaleItem configuredSaleItem = new ConfiguredSaleItem();
        configuredSaleItem.setConfiguredTemplateId(template.getId());
        configuredSaleItem.setParentMenuItemId(parentMenuItem.getId());
        configuredSaleItem.setLineName(parentMenuItem.getItemName());
        configuredSaleItem.setBasePrice(basePrice);
        configuredSaleItem.setAmount(orderItemRequest.amount());
        configuredSaleItem.setRestaurantId(parentMenuItem.getRestaurantId());

        Map<Long, OrderV2InitiateRequest.SlotItemRequest> selectionBySlotId = mapSlotItemsBySlot(
                orderItemRequest.configuration().slotItems()
        );
        validateNoUnknownSlots(template, selectionBySlotId.keySet());

        List<ConfiguredSaleItemSelection> nextSelections = new ArrayList<>();
        List<StockRequest> stockRequests = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        Set<Long> affectedMenuIds = new LinkedHashSet<>();
        List<TaxableChargeComponent> components = new ArrayList<>();

        BigDecimal quantity = BigDecimal.valueOf(orderItemRequest.amount());
        components.add(new TaxableChargeComponent(
                lineKey + ":base",
                parentTaxClass.getCode(),
                parentTaxClass.getId(),
                MoneyUtils.multiply(basePrice, quantity),
                Boolean.TRUE.equals(parentMenuItem.getItemPrice().getPriceIncludesTax())
        ));

        for (ConfiguredMenuSlot slot : template.getSlots()) {
            List<OrderV2InitiateRequest.SlotItemQuantityRequest> slotItems = selectionBySlotId.containsKey(slot.getId())
                    ? selectionBySlotId.get(slot.getId()).items()
                    : List.of();

            Map<Long, ConfiguredMenuOption> optionsByChildId = new LinkedHashMap<>();
            for (ConfiguredMenuOption option : slot.getOptions()) {
                optionsByChildId.put(option.getChildMenuItem().getId(), option);
            }
            validateSlotItems(slot, slotItems, optionsByChildId);

            for (OrderV2InitiateRequest.SlotItemQuantityRequest slotItem : slotItems) {
                ConfiguredMenuOption option = optionsByChildId.get(slotItem.childMenuItemId());
                if (InventoryAvailabilityUtil.resolveMenuType(option.getChildMenuItem()) == MenuType.CONFIGURABLE) {
                    throw new AppException("Configurable menu items cannot be nested as child options", HttpStatus.BAD_REQUEST);
                }

                BigDecimal optionDelta = calculateOptionDelta(option, slotItem.quantity());
                optionDeltaTotal = MoneyUtils.add(optionDeltaTotal, optionDelta);
                ConfiguredSaleItemSelection selection = new ConfiguredSaleItemSelection();
                selection.setConfiguredSaleItem(configuredSaleItem);
                selection.setSlotId(slot.getId());
                selection.setSlotName(slot.getSlotName());
                selection.setChildMenuItemId(option.getChildMenuItem().getId());
                selection.setChildItemName(option.getChildMenuItem().getItemName());
                selection.setQuantity(slotItem.quantity());
                selection.setPriceDelta(option.getPriceDelta());
                nextSelections.add(selection);

                MenuItem selectedMenuItem = inventoryService.getAccessibleMenuItem(option.getChildMenuItem().getId());
                TaxClass childTaxClass = taxService.resolveTaxClass(selectedMenuItem.getRestaurantId(), selectedMenuItem.getTaxClassId());
                InventoryAvailabilityUtil.accumulateStockRequirements(
                        selectedMenuItem,
                        scaleSelectionAmount(orderItemRequest.amount(), slotItem.quantity()),
                        stockRequests,
                        ingredientRequirements,
                        preparedRequirements
                );
                affectedMenuIds.add(selectedMenuItem.getId());
                if (optionDelta.compareTo(BigDecimal.ZERO) > 0) {
                    components.add(new TaxableChargeComponent(
                            lineKey + ":option:" + slot.getId() + ":" + selectedMenuItem.getId(),
                            childTaxClass.getCode(),
                            childTaxClass.getId(),
                            MoneyUtils.multiply(optionDelta, quantity),
                            Boolean.TRUE.equals(selectedMenuItem.getItemPrice().getPriceIncludesTax())
                    ));
                }
            }
        }

        configuredSaleItem.getSelections().clear();
        configuredSaleItem.getSelections().addAll(nextSelections);
        configuredSaleItem.setUnitPrice(MoneyUtils.add(basePrice, optionDeltaTotal));
        BigDecimal unitListAmount = MoneyUtils.add(MoneyUtils.money(parentMenuItem.getItemPrice().getPrice()), optionDeltaTotal);
        BigDecimal unitDiscountAmount = MoneyUtils.subtract(unitListAmount, configuredSaleItem.getUnitPrice());
        return new ConfiguredLineDraft(
                configuredSaleItem,
                OrderPricingService.LinePricingPlan.forConfiguredSaleItem(
                        lineKey,
                        components.stream().map(TaxableChargeComponent::taxClassCode).distinct().count() == 1
                                ? components.get(0).taxClassCode()
                                : "MIXED",
                        components.stream().anyMatch(TaxableChargeComponent::priceIncludesTax),
                        unitListAmount,
                        unitDiscountAmount,
                        MoneyUtils.multiply(unitListAmount, quantity),
                        MoneyUtils.multiply(unitDiscountAmount, quantity),
                        components,
                        configuredSaleItem
                ),
                new StockRequirements(stockRequests, ingredientRequirements, preparedRequirements, affectedMenuIds)
        );
    }

    private StockRequirements buildPersistedStockRequirements(Order order) {
        List<StockRequest> stockRequests = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        Set<Long> affectedMenuIds = new LinkedHashSet<>();

        for (SaleItem saleItem : order.getOrderItemList()) {
            MenuItem menuItem = inventoryService.getAccessibleMenuItem(saleItem.getMenuItem().getId());
            InventoryAvailabilityUtil.accumulateStockRequirements(
                    menuItem,
                    saleItem.getAmount(),
                    stockRequests,
                    ingredientRequirements,
                    preparedRequirements
            );
            affectedMenuIds.add(menuItem.getId());
        }

        for (ConfiguredSaleItem configuredItem : configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId())) {
            for (ConfiguredSaleItemSelection selection : configuredItem.getSelections()) {
                MenuItem selectedMenuItem = inventoryService.getAccessibleMenuItem(selection.getChildMenuItemId());
                InventoryAvailabilityUtil.accumulateStockRequirements(
                    selectedMenuItem,
                    scaleSelectionAmount(configuredItem.getAmount(), selection.getQuantity()),
                    stockRequests,
                    ingredientRequirements,
                    preparedRequirements
                );
                affectedMenuIds.add(selectedMenuItem.getId());
            }
        }

        return new StockRequirements(stockRequests, ingredientRequirements, preparedRequirements, affectedMenuIds);
    }

    private void applyOrderDraft(Order order, OrderV2Draft draft) {
        replaceOrderItems(order, draft.saleItems());
        order.setRestaurantId(draft.restaurantId());
        orderPricingService.applyPricing(order, draft.restaurantId(), draft.linePlans(), draft.taxContextRequest());
    }

    private void replaceOrderItems(Order order, List<SaleItem> saleItems) {
        List<SaleItem> managedItems = order.getOrderItemList();
        if (managedItems == null) {
            managedItems = new ArrayList<>();
            order.setOrderItemList(managedItems);
        }
        managedItems.clear();
        for (SaleItem saleItem : saleItems) {
            saleItem.setOrder(order);
        }
        managedItems.addAll(saleItems);
    }

    private void persistConfiguredSaleItems(Order order, List<ConfiguredSaleItem> configuredItems) {
        for (ConfiguredSaleItem configuredItem : configuredItems) {
            configuredItem.setOrder(order);
            for (ConfiguredSaleItemSelection selection : configuredItem.getSelections()) {
                selection.setConfiguredSaleItem(configuredItem);
            }
        }
        configuredSaleItemRepository.saveAll(configuredItems);
    }

    private void replaceConfiguredSaleItems(Order order, List<ConfiguredSaleItem> configuredItems) {
        List<ConfiguredSaleItem> existingItems = configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId());
        if (!existingItems.isEmpty()) {
            configuredSaleItemRepository.deleteAll(existingItems);
        }
        persistConfiguredSaleItems(order, configuredItems);
    }

    private Map<Long, OrderV2InitiateRequest.SlotItemRequest> mapSlotItemsBySlot(
            List<OrderV2InitiateRequest.SlotItemRequest> slotItems
    ) {
        Map<Long, OrderV2InitiateRequest.SlotItemRequest> itemsBySlotId = new LinkedHashMap<>();
        if (slotItems == null) {
            return itemsBySlotId;
        }
        for (OrderV2InitiateRequest.SlotItemRequest slotItem : slotItems) {
            if (itemsBySlotId.put(slotItem.slotId(), slotItem) != null) {
                throw new AppException("Duplicate slot entry provided", HttpStatus.BAD_REQUEST);
            }
        }
        return itemsBySlotId;
    }

    private void validateNoUnknownSlots(ConfiguredMenuTemplate template, Set<Long> providedSlotIds) {
        Set<Long> validSlotIds = template.getSlots().stream()
                .map(ConfiguredMenuSlot::getId)
                .collect(java.util.stream.Collectors.toSet());
        for (Long providedSlotId : providedSlotIds) {
            if (!validSlotIds.contains(providedSlotId)) {
                throw new AppException("Invalid slot entry for configured template", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateSlotItems(ConfiguredMenuSlot slot,
                                   List<OrderV2InitiateRequest.SlotItemQuantityRequest> slotItems,
                                   Map<Long, ConfiguredMenuOption> optionsByChildId) {
        Set<Long> seenOptionIds = new HashSet<>();
        for (OrderV2InitiateRequest.SlotItemQuantityRequest slotItem : slotItems) {
            if (!seenOptionIds.add(slotItem.childMenuItemId())) {
                throw new AppException("Duplicate slot item in slot " + slot.getSlotName(), HttpStatus.BAD_REQUEST);
            }
            ConfiguredMenuOption option = optionsByChildId.get(slotItem.childMenuItemId());
            if (option == null) {
                throw new AppException("Slot item is not allowed for slot " + slot.getSlotName(), HttpStatus.BAD_REQUEST);
            }
            int minimumQuantity = effectiveMinQuantity(option);
            if (slotItem.quantity() < minimumQuantity) {
                throw new AppException(
                        "Slot item " + option.getChildMenuItem().getItemName() + " requires at least " + minimumQuantity + " quantity",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        int selectedCount = seenOptionIds.size();
        if (slot.getMinSelections() == null || slot.getMaxSelections() == null) {
            throw new AppException("Slot " + slot.getSlotName() + " is missing selection rules", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (selectedCount < slot.getMinSelections()) {
            throw new AppException("Slot " + slot.getSlotName() + " requires at least " + slot.getMinSelections() + " selection", HttpStatus.BAD_REQUEST);
        }
        if (selectedCount > slot.getMaxSelections()) {
            throw new AppException("Slot " + slot.getSlotName() + " allows at most " + slot.getMaxSelections() + " selections", HttpStatus.BAD_REQUEST);
        }
        if (Boolean.TRUE.equals(slot.getIsRequired()) && selectedCount == 0) {
            throw new AppException("Slot " + slot.getSlotName() + " is required", HttpStatus.BAD_REQUEST);
        }
    }

    private int scaleSelectionAmount(Integer lineAmount, Integer quantity) {
        return Math.multiplyExact(lineAmount, quantity);
    }

    private BigDecimal calculateOptionDelta(ConfiguredMenuOption option, Integer quantity) {
        int chargeableQuantity = Math.max(0, quantity - effectiveMinQuantity(option));
        return option.getPriceDelta().multiply(BigDecimal.valueOf(chargeableQuantity))
                .setScale(MoneyUtils.MONEY_SCALE, java.math.RoundingMode.HALF_UP);
    }

    private int effectiveMinQuantity(ConfiguredMenuOption option) {
        return option.getMinQuantity() == null ? 0 : option.getMinQuantity();
    }

    private Long validateRestaurant(Order order, Long orderRestaurantId, Long menuRestaurantId) {
        if (orderRestaurantId == null) {
            orderRestaurantId = menuRestaurantId;
        } else if (!orderRestaurantId.equals(menuRestaurantId)) {
            throw new AppException("All order items must belong to the same restaurant", HttpStatus.BAD_REQUEST);
        }

        if (order.getRestaurantId() != null && !order.getRestaurantId().equals(orderRestaurantId)) {
            throw new AppException("Cart items must stay in the same restaurant as the original order", HttpStatus.BAD_REQUEST);
        }
        return orderRestaurantId;
    }

    private void mergeRequirements(List<StockRequest> stockRequests,
                                   Map<String, Double> ingredientRequirements,
                                   Map<Long, Double> preparedRequirements,
                                   Set<Long> affectedMenuIds,
                                   StockRequirements additionalRequirements) {
        stockRequests.addAll(additionalRequirements.stockRequests());
        additionalRequirements.ingredientRequirements().forEach(
                (sku, quantity) -> ingredientRequirements.merge(sku, quantity, Double::sum)
        );
        additionalRequirements.preparedRequirements().forEach(
                (menuItemId, quantity) -> preparedRequirements.merge(menuItemId, quantity, Double::sum)
        );
        affectedMenuIds.addAll(additionalRequirements.affectedMenuIds());
    }

    private PaymentType resolvePaymentType(PaymentType requestedPaymentType, PaymentType fallbackPaymentType) {
        return requestedPaymentType == null ? fallbackPaymentType : requestedPaymentType;
    }

    private void applyPaymentAudit(Order order, CompletePaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setPaymentReference(trimToNull(request == null ? null : request.getPaymentReference()));
        order.setPaymentCollectedBy(trimToNull(
                request != null && request.getPaymentCollectedBy() != null
                        ? request.getPaymentCollectedBy()
                        : currentUser != null ? currentUser.getEmail() : null
        ));
        order.setPaymentNotes(trimToNull(request == null ? null : request.getPaymentNotes()));
        order.setExternalTxnId(trimToNull(request == null ? null : request.getExternalTxnId()));
    }

    private void applyRefundAudit(Order order, RefundPaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setRefundOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setRefundReason(trimToNull(request == null ? null : request.getRefundReason()));
        order.setRefundNotes(trimToNull(request == null ? null : request.getRefundNotes()));
    }

    private SecurityUser resolveCurrentUser() {
        try {
            return tenantAccessService.currentUser();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return InventoryUtil.trimToNull(value);
    }

    private Order getAccessibleOrderWithItemsForUpdate(String orderId) {
        Order order = orderRepository.findByOrderIdWithItemsForUpdate(orderId).orElseThrow(OrderException::new);
        validateAccessibleOrder(order);
        return order;
    }

    private void validateAccessibleOrder(Order order) {
        if (order.isDeleted()) {
            throw new OrderException("Order not found");
        }
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(order.getRestaurantId());
        }
    }

    private OrderV2Response buildResponse(Order order, String message, String description) {
        List<OrderSaleItemResponse> saleItems = order.getOrderItemList().stream()
                .map(OrderSaleItemResponse::fromEntity)
                .toList();
        List<ConfiguredSaleItemResponse> configuredItems = configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId()).stream()
                .map(ConfiguredSaleItemResponse::fromEntity)
                .toList();
        return new OrderV2Response(order, saleItems, configuredItems, message, description);
    }

    private record OrderV2Draft(
            Long restaurantId,
            List<SaleItem> saleItems,
            List<ConfiguredSaleItem> configuredItems,
            List<OrderPricingService.LinePricingPlan> linePlans,
            com.kritik.POS.order.model.request.OrderTaxContextRequest taxContextRequest
    ) {
    }

    private record ConfiguredLineDraft(
            ConfiguredSaleItem item,
            OrderPricingService.LinePricingPlan pricingPlan,
            StockRequirements requirements
    ) {
    }

    private record StockRequirements(
            List<StockRequest> stockRequests,
            Map<String, Double> ingredientRequirements,
            Map<Long, Double> preparedRequirements,
            Set<Long> affectedMenuIds
    ) {
    }
}
