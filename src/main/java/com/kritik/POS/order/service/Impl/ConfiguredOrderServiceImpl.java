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
import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import com.kritik.POS.order.model.request.CompletePaymentRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderInitiateRequest;
import com.kritik.POS.order.model.request.ConfiguredOrderUpdateRequest;
import com.kritik.POS.order.model.request.RefundPaymentRequest;
import com.kritik.POS.order.model.response.ConfiguredOrderResponse;
import com.kritik.POS.order.model.response.ConfiguredSaleItemResponse;
import com.kritik.POS.order.repository.ConfiguredSaleItemRepository;
import com.kritik.POS.order.repository.OrderRepository;
import com.kritik.POS.order.service.OrderPricingService;
import com.kritik.POS.order.service.ConfiguredOrderService;
import com.kritik.POS.restaurant.entity.MenuItem;
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
public class ConfiguredOrderServiceImpl implements ConfiguredOrderService {

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
                "Configured order refunded successfully and stock has been restored."
        );
    }

    private ConfiguredOrderDraft buildConfiguredOrderDraft(Order order,
                                                           List<ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest> orderItems,
                                                           com.kritik.POS.order.model.request.OrderTaxContextRequest taxContextRequest) {
        List<ConfiguredSaleItem> configuredItems = new ArrayList<>();
        List<StockRequest> stockRequests = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        Set<Long> affectedMenuIds = new LinkedHashSet<>();
        List<OrderPricingService.LinePricingPlan> linePlans = new ArrayList<>();
        Long orderRestaurantId = null;
        int lineIndex = 0;

        for (ConfiguredOrderInitiateRequest.ConfiguredOrderItemRequest orderItemRequest : orderItems) {
            ConfiguredMenuTemplate template = getAccessibleActiveTemplate(orderItemRequest.configuredMenuTemplateId());
            orderRestaurantId = validateRestaurant(order, orderRestaurantId, template.getRestaurantId());

            TaxClass parentTaxClass = taxService.resolveTaxClass(template.getRestaurantId(), template.getParentMenuItem().getTaxClassId());
            BigDecimal basePrice = RestaurantUtil.getMenuItemPrice(template.getParentMenuItem().getItemPrice());
            BigDecimal optionDeltaTotal = MoneyUtils.zero();
            ConfiguredSaleItem configuredSaleItem = new ConfiguredSaleItem();
            configuredSaleItem.setConfiguredTemplateId(template.getId());
            configuredSaleItem.setParentMenuItemId(template.getParentMenuItem().getId());
            configuredSaleItem.setLineName(template.getParentMenuItem().getItemName());
            configuredSaleItem.setBasePrice(basePrice);
            configuredSaleItem.setAmount(orderItemRequest.amount());
            configuredSaleItem.setRestaurantId(template.getRestaurantId());

            Map<Long, ConfiguredOrderInitiateRequest.SlotItemRequest> selectionBySlotId = mapSlotItemsBySlot(orderItemRequest.slotItems());
            validateNoUnknownSlots(template, selectionBySlotId.keySet());

            List<ConfiguredSaleItemSelection> nextSelections = new ArrayList<>();
            List<TaxableChargeComponent> components = new ArrayList<>();
            BigDecimal quantity = BigDecimal.valueOf(orderItemRequest.amount());
            String lineKey = "cfg-" + lineIndex++;
            components.add(new TaxableChargeComponent(
                    lineKey + ":base",
                    parentTaxClass.getCode(),
                    parentTaxClass.getId(),
                    MoneyUtils.multiply(basePrice, quantity),
                    Boolean.TRUE.equals(template.getParentMenuItem().getItemPrice().getPriceIncludesTax())
            ));
            for (ConfiguredMenuSlot slot : template.getSlots()) {
                List<ConfiguredOrderInitiateRequest.SlotItemQuantityRequest> slotItems = selectionBySlotId.containsKey(slot.getId())
                        ? selectionBySlotId.get(slot.getId()).items()
                        : List.of();

                Map<Long, ConfiguredMenuOption> optionsByChildId = new LinkedHashMap<>();
                for (ConfiguredMenuOption option : slot.getOptions()) {
                    optionsByChildId.put(option.getChildMenuItem().getId(), option);
                }
                validateSlotItems(slot, slotItems, optionsByChildId);

                for (ConfiguredOrderInitiateRequest.SlotItemQuantityRequest slotItem : slotItems) {
                    ConfiguredMenuOption option = optionsByChildId.get(slotItem.childMenuItemId());

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
                    if (!template.getRestaurantId().equals(selectedMenuItem.getRestaurantId())) {
                        throw new AppException("Configured option menu item does not belong to the order restaurant", HttpStatus.BAD_REQUEST);
                    }
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

            configuredSaleItem.setUnitPrice(MoneyUtils.add(basePrice, optionDeltaTotal));
            configuredSaleItem.getSelections().clear();
            configuredSaleItem.getSelections().addAll(nextSelections);
            configuredItems.add(configuredSaleItem);
            BigDecimal unitListAmount = MoneyUtils.add(MoneyUtils.money(template.getParentMenuItem().getItemPrice().getPrice()), optionDeltaTotal);
            BigDecimal unitDiscountAmount = MoneyUtils.subtract(unitListAmount, configuredSaleItem.getUnitPrice());
            linePlans.add(OrderPricingService.LinePricingPlan.forConfiguredSaleItem(
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
            ));
        }

        inventoryService.checkOrderStockAvailability(stockRequests, ingredientRequirements, preparedRequirements);
        return new ConfiguredOrderDraft(
                orderRestaurantId,
                configuredItems,
                linePlans,
                taxContextRequest,
                stockRequests,
                ingredientRequirements,
                preparedRequirements,
                affectedMenuIds
        );
    }

    private StockRequirements buildPersistedStockRequirements(Order order) {
        List<ConfiguredSaleItem> configuredItems = configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId());
        List<StockRequest> stockRequests = new ArrayList<>();
        Map<String, Double> ingredientRequirements = new HashMap<>();
        Map<Long, Double> preparedRequirements = new HashMap<>();
        Set<Long> affectedMenuIds = new LinkedHashSet<>();

        for (ConfiguredSaleItem configuredItem : configuredItems) {
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
                                   List<ConfiguredOrderInitiateRequest.SlotItemQuantityRequest> slotItems,
                                   Map<Long, ConfiguredMenuOption> optionsByChildId) {
        Set<Long> seenOptionIds = new HashSet<>();
        for (ConfiguredOrderInitiateRequest.SlotItemQuantityRequest slotItem : slotItems) {
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

    private ConfiguredMenuTemplate getAccessibleActiveTemplate(Long templateId) {
        ConfiguredMenuTemplate template = configuredMenuTemplateRepository.findById(templateId)
                .orElseThrow(() -> new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST));
        if (Boolean.TRUE.equals(template.getIsDeleted()) || !Boolean.TRUE.equals(template.getIsActive())) {
            throw new AppException("Configured menu template is inactive", HttpStatus.BAD_REQUEST);
        }
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(template.getRestaurantId());
        }
        return template;
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

    private ConfiguredOrderResponse buildResponse(Order order, String message, String description) {
        List<ConfiguredSaleItemResponse> items = configuredSaleItemRepository.findAllByOrder_IdOrderByIdAsc(order.getId()).stream()
                .map(ConfiguredSaleItemResponse::fromEntity)
                .toList();
        return new ConfiguredOrderResponse(order, items, message, description);
    }

    private record ConfiguredOrderDraft(Long restaurantId,
                                        List<ConfiguredSaleItem> items,
                                        List<OrderPricingService.LinePricingPlan> linePlans,
                                        com.kritik.POS.order.model.request.OrderTaxContextRequest taxContextRequest,
                                        List<StockRequest> stockRequests,
                                        Map<String, Double> ingredientRequirements,
                                        Map<Long, Double> preparedRequirements,
                                        Set<Long> affectedMenuIds) {
    }

    private record StockRequirements(List<StockRequest> stockRequests,
                                     Map<String, Double> ingredientRequirements,
                                     Map<Long, Double> preparedRequirements,
                                     Set<Long> affectedMenuIds) {
    }
}
