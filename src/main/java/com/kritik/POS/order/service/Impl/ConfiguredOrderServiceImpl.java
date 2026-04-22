package com.kritik.POS.order.service.Impl;

import com.kritik.POS.common.util.MoneyUtils;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuApi;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuOptionSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuSlotSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuTemplateSnapshot;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.exception.errors.OrderException;
import com.kritik.POS.inventory.api.InventoryApi;
import com.kritik.POS.order.api.OrderCompletedEvent;
import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.ConfiguredSaleItemSelection;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderUpdateRequest;
import com.kritik.POS.order.model.request.OrderTaxContextRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;
import com.kritik.POS.order.model.response.ConfiguredSaleItemResponse;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.ConfiguredOrderService;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.order.service.internal.ConfiguredSelectionInput;
import com.kritik.POS.order.service.internal.OrderMenuSupport;
import com.kritik.POS.order.service.internal.OrderStockDemand;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import com.kritik.POS.restaurant.api.MenuItemType;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.api.TaxApi;
import com.kritik.POS.tax.api.TaxClassSnapshot;
import com.kritik.POS.tax.api.TaxableChargeComponent;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguredOrderServiceImpl implements ConfiguredOrderService {

    private final OrderRepository orderRepository;
    private final ConfiguredSaleItemRepository configuredSaleItemRepository;
    private final ConfiguredMenuApi configuredMenuApi;
    private final MenuCatalogApi menuCatalogApi;
    private final InventoryApi inventoryApi;
    private final TaxApi taxApi;
    private final OrderPricingService orderPricingService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public ConfiguredOrderResponse initiateOrder(ConfiguredOrderInitiateRequest request) {
        Order order = new Order();
        ConfiguredOrderDraft draft = buildConfiguredOrderDraft(order, request.getOrderItems(), request.getTaxContext());
        applyOrderDraft(order, draft);
        order.setPaymentType(resolvePaymentType(request.getPaymentType(), PaymentType.CASH));
        order.setPaymentInitiatedTime(LocalDateTime.now());
        order.setOrderId(UUID.randomUUID().toString());

        Order savedOrder = orderRepository.save(order);
        persistConfiguredSaleItems(savedOrder, draft.items());
        return buildResponse(
                savedOrder,
                "Payment Initiation Complete",
                "Your configured order is ready for payment. Please select your preferred payment method to continue."
        );
    }

    @Override
    @Transactional
    public ConfiguredOrderResponse updateOrder(String orderId, ConfiguredOrderUpdateRequest request) {
        Order order = getAccessibleConfiguredOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cart can only be updated before payment confirmation", HttpStatus.BAD_REQUEST);
        }

        ConfiguredOrderDraft draft = buildConfiguredOrderDraft(order, request.getOrderItems(), request.getTaxContext());
        applyOrderDraft(order, draft);
        order.setPaymentType(resolvePaymentType(request.getPaymentType(), order.getPaymentType()));
        Order savedOrder = orderRepository.save(order);
        replaceConfiguredSaleItems(savedOrder, draft.items());
        return buildResponse(
                savedOrder,
                "Order Updated",
                "Your configured cart has been updated while payment is still pending confirmation."
        );
    }

    @Override
    @Transactional
    public ConfiguredOrderResponse cancelTransaction(String orderId) {
        Order order = getAccessibleConfiguredOrderWithItemsForUpdate(orderId);
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Cant cancel at this moment");
        }
        order.setPaymentStatus(PaymentStatus.PAYMENT_CANCELED);
        order.setCancelledAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        return buildResponse(
                savedOrder,
                "Payment Canceled",
                "Your configured order payment has been canceled. You can update the slot items and try again."
        );
    }

    @Override
    @Transactional
    public ConfiguredOrderResponse completePayment(String orderId, CompletePaymentRequest request) {
        Order order = getAccessibleConfiguredOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_SUCCESSFUL)) {
            return buildResponse(
                    order,
                    "Payment Already Completed",
                    "This configured order was already marked as paid. Returning the existing payment result."
            );
        }
        if (!order.getPaymentStatus().equals(PaymentStatus.PAYMENT_INITIATED)) {
            throw new OrderException("Payment cannot be completed in the current state");
        }

        OrderStockDemand demand = buildPersistedStockRequirements(order);
        inventoryApi.checkOrderStockAvailability(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements()
        );
        inventoryApi.deductStockForRequirements(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements(),
                demand.affectedMenuIds()
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
                "Your configured order transaction is complete. Thank you for your purchase!"
        );
    }

    @Override
    @Transactional
    public ConfiguredOrderResponse refundPayment(String orderId, RefundPaymentRequest request) {
        Order order = getAccessibleConfiguredOrderWithItemsForUpdate(orderId);
        if (order.getPaymentStatus().equals(PaymentStatus.PAYMENT_REFUND)) {
            return buildResponse(
                    order,
                    "Order Already Refunded",
                    "This configured order has already been refunded. Returning the current refund state."
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

        OrderStockDemand demand = buildPersistedStockRequirements(order);
        inventoryApi.restoreStockForRequirements(
                demand.stockRequests(),
                demand.ingredientRequirements(),
                demand.preparedRequirements(),
                demand.affectedMenuIds()
        );
        order.setPaymentStatus(PaymentStatus.PAYMENT_REFUND);
        order.setRefundedAt(LocalDateTime.now());
        applyRefundAudit(order, request);
        Order savedOrder = orderRepository.save(order);
        return buildResponse(
                savedOrder,
                "Order Refunded",
                "Configured order refunded successfully and stock has been restored."
        );
    }

    private ConfiguredOrderDraft buildConfiguredOrderDraft(Order order,
                                                           List<ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest> orderItems,
                                                           OrderTaxContextRequest taxContextRequest) {
        List<ConfiguredSaleItem> configuredItems = new ArrayList<>();
        List<OrderPricingService.LinePricingPlan> linePlans = new ArrayList<>();
        OrderStockDemand demand = OrderStockDemand.empty();
        Long orderRestaurantId = null;
        int lineIndex = 0;

        for (ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest orderItemRequest : orderItems) {
            ConfiguredLineDraft configuredLineDraft = buildConfiguredLine(
                    configuredMenuApi.getAccessibleActiveTemplate(orderItemRequest.configuredMenuTemplateId()),
                    orderItemRequest.amount(),
                    orderItemRequest.slotItems(),
                    "cfg-" + lineIndex++
            );
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, configuredLineDraft.item().getRestaurantId());
            configuredItems.add(configuredLineDraft.item());
            linePlans.add(configuredLineDraft.pricingPlan());
            OrderMenuSupport.mergeDemands(demand, configuredLineDraft.demand());
        }

        inventoryApi.checkOrderStockAvailability(demand.stockRequests(), demand.ingredientRequirements(), demand.preparedRequirements());
        return new ConfiguredOrderDraft(orderRestaurantId, configuredItems, linePlans, taxContextRequest);
    }

    private ConfiguredLineDraft buildConfiguredLine(ConfiguredMenuTemplateSnapshot template,
                                                    Integer amount,
                                                    List<ConfiguredOrderInitiateRequest.SlotItemRequest> slotItems,
                                                    String lineKey) {
        MenuItemSnapshot parentMenuItem = template.parentMenuItem();
        TaxClassSnapshot parentTaxClass = taxApi.resolveTaxClass(template.restaurantId(), parentMenuItem.taxClassId());
        BigDecimal basePrice = parentMenuItem.price().discountedPrice();
        BigDecimal optionDeltaTotal = MoneyUtils.zero();
        ConfiguredSaleItem configuredSaleItem = new ConfiguredSaleItem();
        configuredSaleItem.setConfiguredTemplateId(template.id());
        configuredSaleItem.setParentMenuItemId(parentMenuItem.id());
        configuredSaleItem.setLineName(parentMenuItem.itemName());
        configuredSaleItem.setBasePrice(basePrice);
        configuredSaleItem.setAmount(amount);
        configuredSaleItem.setRestaurantId(template.restaurantId());

        Map<Long, ConfiguredOrderInitiateRequest.SlotItemRequest> selectionBySlotId = mapSlotItemsBySlot(slotItems);
        validateNoUnknownSlots(template, selectionBySlotId.keySet());

        List<ConfiguredSaleItemSelection> selections = new ArrayList<>();
        List<TaxableChargeComponent> components = new ArrayList<>();
        OrderStockDemand demand = OrderStockDemand.empty();
        BigDecimal quantity = BigDecimal.valueOf(amount);
        components.add(new TaxableChargeComponent(
                lineKey + ":base",
                parentTaxClass.code(),
                parentTaxClass.id(),
                MoneyUtils.multiply(basePrice, quantity),
                parentMenuItem.price().priceIncludesTax()
        ));

        for (ConfiguredMenuSlotSnapshot slot : template.slots()) {
            List<ConfiguredOrderInitiateRequest.SlotItemQuantityRequest> selectedItems = selectionBySlotId.containsKey(slot.id())
                    ? selectionBySlotId.get(slot.id()).items()
                    : List.of();
            Map<Long, ConfiguredMenuOptionSnapshot> optionsByChildId = slot.options().stream()
                    .collect(Collectors.toMap(
                            ConfiguredMenuOptionSnapshot::childMenuItemId,
                            option -> option,
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
            validateSlotItems(slot, selectedItems, optionsByChildId);

            for (ConfiguredOrderInitiateRequest.SlotItemQuantityRequest slotItem : selectedItems) {
                ConfiguredMenuOptionSnapshot option = optionsByChildId.get(slotItem.childMenuItemId());
                MenuItemSnapshot childMenuItem = menuCatalogApi.getAccessibleMenuItem(option.childMenuItemId());
                if (OrderMenuSupport.resolveMenuType(childMenuItem) == MenuItemType.CONFIGURABLE) {
                    throw new AppException("Configurable menu items cannot be nested as child options", HttpStatus.BAD_REQUEST);
                }
                if (!template.restaurantId().equals(childMenuItem.restaurantId())) {
                    throw new AppException("Configured option menu item does not belong to the order restaurant", HttpStatus.BAD_REQUEST);
                }

                BigDecimal optionDelta = calculateOptionDelta(option, slotItem.quantity());
                optionDeltaTotal = MoneyUtils.add(optionDeltaTotal, optionDelta);
                selections.add(toSelectionEntity(new ConfiguredSelectionInput(
                        slot.id(),
                        slot.slotName(),
                        option.childMenuItemId(),
                        option.childItemName(),
                        slotItem.quantity(),
                        option.priceDelta()
                ), configuredSaleItem));

                TaxClassSnapshot childTaxClass = taxApi.resolveTaxClass(childMenuItem.restaurantId(), childMenuItem.taxClassId());
                OrderMenuSupport.accumulateStockRequirements(
                        childMenuItem,
                        scaleSelectionAmount(amount, slotItem.quantity()),
                        demand
                );
                if (optionDelta.compareTo(BigDecimal.ZERO) > 0) {
                    components.add(new TaxableChargeComponent(
                            lineKey + ":option:" + slot.id() + ":" + childMenuItem.id(),
                            childTaxClass.code(),
                            childTaxClass.id(),
                            MoneyUtils.multiply(optionDelta, quantity),
                            childMenuItem.price().priceIncludesTax()
                    ));
                }
            }
        }

        configuredSaleItem.setUnitPrice(MoneyUtils.add(basePrice, optionDeltaTotal));
        configuredSaleItem.getSelections().clear();
        configuredSaleItem.getSelections().addAll(selections);
        BigDecimal unitListAmount = MoneyUtils.add(parentMenuItem.price().listPrice(), optionDeltaTotal);
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
                demand
        );
    }

    private OrderStockDemand buildPersistedStockRequirements(Order order) {
        OrderStockDemand demand = OrderStockDemand.empty();
        for (ConfiguredSaleItem configuredItem : configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId())) {
            for (ConfiguredSaleItemSelection selection : configuredItem.getSelections()) {
                MenuItemSnapshot childMenuItem = menuCatalogApi.getAccessibleMenuItem(selection.getChildMenuItemId());
                OrderMenuSupport.accumulateStockRequirements(
                        childMenuItem,
                        scaleSelectionAmount(configuredItem.getAmount(), selection.getQuantity()),
                        demand
                );
            }
        }
        return demand;
    }

    private Map<Long, ConfiguredOrderInitiateRequest.SlotItemRequest> mapSlotItemsBySlot(
            List<ConfiguredOrderInitiateRequest.SlotItemRequest> slotItems
    ) {
        Map<Long, ConfiguredOrderInitiateRequest.SlotItemRequest> itemsBySlotId = new LinkedHashMap<>();
        if (slotItems == null) {
            return itemsBySlotId;
        }
        for (ConfiguredOrderInitiateRequest.SlotItemRequest slotItem : slotItems) {
            if (itemsBySlotId.put(slotItem.slotId(), slotItem) != null) {
                throw new AppException("Duplicate slot entry provided", HttpStatus.BAD_REQUEST);
            }
        }
        return itemsBySlotId;
    }

    private void validateNoUnknownSlots(ConfiguredMenuTemplateSnapshot template, Set<Long> providedSlotIds) {
        Set<Long> validSlotIds = template.slots().stream()
                .map(ConfiguredMenuSlotSnapshot::id)
                .collect(Collectors.toSet());
        for (Long providedSlotId : providedSlotIds) {
            if (!validSlotIds.contains(providedSlotId)) {
                throw new AppException("Invalid slot entry for configured template", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateSlotItems(ConfiguredMenuSlotSnapshot slot,
                                   List<ConfiguredOrderInitiateRequest.SlotItemQuantityRequest> slotItems,
                                   Map<Long, ConfiguredMenuOptionSnapshot> optionsByChildId) {
        Set<Long> seenOptionIds = new LinkedHashSet<>();
        for (ConfiguredOrderInitiateRequest.SlotItemQuantityRequest slotItem : slotItems) {
            if (!seenOptionIds.add(slotItem.childMenuItemId())) {
                throw new AppException("Duplicate slot item in slot " + slot.slotName(), HttpStatus.BAD_REQUEST);
            }
            ConfiguredMenuOptionSnapshot option = optionsByChildId.get(slotItem.childMenuItemId());
            if (option == null) {
                throw new AppException("Slot item is not allowed for slot " + slot.slotName(), HttpStatus.BAD_REQUEST);
            }
            int minimumQuantity = effectiveMinQuantity(option);
            if (slotItem.quantity() < minimumQuantity) {
                throw new AppException(
                        "Slot item " + option.childItemName() + " requires at least " + minimumQuantity + " quantity",
                        HttpStatus.BAD_REQUEST
                );
            }
        }

        int selectedCount = seenOptionIds.size();
        if (slot.minSelections() == null || slot.maxSelections() == null) {
            throw new AppException("Slot " + slot.slotName() + " is missing selection rules", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (selectedCount < slot.minSelections()) {
            throw new AppException("Slot " + slot.slotName() + " requires at least " + slot.minSelections() + " selection", HttpStatus.BAD_REQUEST);
        }
        if (selectedCount > slot.maxSelections()) {
            throw new AppException("Slot " + slot.slotName() + " allows at most " + slot.maxSelections() + " selections", HttpStatus.BAD_REQUEST);
        }
        if (slot.required() && selectedCount == 0) {
            throw new AppException("Slot " + slot.slotName() + " is required", HttpStatus.BAD_REQUEST);
        }
    }

    private ConfiguredSaleItemSelection toSelectionEntity(ConfiguredSelectionInput input, ConfiguredSaleItem configuredSaleItem) {
        ConfiguredSaleItemSelection selection = new ConfiguredSaleItemSelection();
        selection.setConfiguredSaleItem(configuredSaleItem);
        selection.setSlotId(input.slotId());
        selection.setSlotName(input.slotName());
        selection.setChildMenuItemId(input.childMenuItemId());
        selection.setChildItemName(input.childItemName());
        selection.setQuantity(input.quantity());
        selection.setPriceDelta(input.priceDelta());
        return selection;
    }

    private int scaleSelectionAmount(Integer lineAmount, Integer quantity) {
        return Math.multiplyExact(lineAmount, quantity);
    }

    private BigDecimal calculateOptionDelta(ConfiguredMenuOptionSnapshot option, Integer quantity) {
        int chargeableQuantity = Math.max(0, quantity - effectiveMinQuantity(option));
        return option.priceDelta().multiply(BigDecimal.valueOf(chargeableQuantity))
                .setScale(MoneyUtils.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private int effectiveMinQuantity(ConfiguredMenuOptionSnapshot option) {
        return option.minQuantity() == null ? 0 : option.minQuantity();
    }

    private void applyOrderDraft(Order order, ConfiguredOrderDraft draft) {
        order.setRestaurantId(draft.restaurantId());
        orderPricingService.applyPricing(order, draft.restaurantId(), draft.linePlans(), draft.taxContextRequest());
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

    private Order getAccessibleConfiguredOrderWithItemsForUpdate(String orderId) {
        Order order = orderRepository.findByOrderIdWithItemsForUpdate(orderId).orElseThrow(OrderException::new);
        validateAccessibleOrder(order);
        if (!configuredSaleItemRepository.existsByOrder_Id(order.getId())) {
            throw new OrderException("Configured order not found", HttpStatus.BAD_REQUEST);
        }
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

    private PaymentType resolvePaymentType(PaymentType requestedPaymentType, PaymentType fallbackPaymentType) {
        return requestedPaymentType == null ? fallbackPaymentType : requestedPaymentType;
    }

    private void applyPaymentAudit(Order order, CompletePaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setPaymentReference(OrderMenuSupport.trimToNull(request == null ? null : request.getPaymentReference()));
        order.setPaymentCollectedBy(OrderMenuSupport.trimToNull(
                request != null && request.getPaymentCollectedBy() != null
                        ? request.getPaymentCollectedBy()
                        : currentUser != null ? currentUser.getEmail() : null
        ));
        order.setPaymentNotes(OrderMenuSupport.trimToNull(request == null ? null : request.getPaymentNotes()));
        order.setExternalTxnId(OrderMenuSupport.trimToNull(request == null ? null : request.getExternalTxnId()));
    }

    private void applyRefundAudit(Order order, RefundPaymentRequest request) {
        SecurityUser currentUser = resolveCurrentUser();
        order.setRefundOperatorUserId(currentUser != null ? currentUser.getUserId() : null);
        order.setRefundReason(OrderMenuSupport.trimToNull(request == null ? null : request.getRefundReason()));
        order.setRefundNotes(OrderMenuSupport.trimToNull(request == null ? null : request.getRefundNotes()));
    }

    private SecurityUser resolveCurrentUser() {
        try {
            return tenantAccessService.currentUser();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private ConfiguredOrderResponse buildResponse(Order order, String message, String description) {
        List<ConfiguredSaleItemResponse> items = configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId()).stream()
                .map(ConfiguredSaleItemResponse::fromEntity)
                .toList();
        return new ConfiguredOrderResponse(order, items, message, description);
    }

    private record ConfiguredOrderDraft(
            Long restaurantId,
            List<ConfiguredSaleItem> items,
            List<OrderPricingService.LinePricingPlan> linePlans,
            OrderTaxContextRequest taxContextRequest
    ) {
    }

    private record ConfiguredLineDraft(
            ConfiguredSaleItem item,
            OrderPricingService.LinePricingPlan pricingPlan,
            OrderStockDemand demand
    ) {
    }
}
