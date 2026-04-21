package com.kritik.POS.order.service;

import com.kritik.POS.order.entity.ConfiguredSaleItem;
import com.kritik.POS.order.entity.Order;
import com.kritik.POS.order.entity.OrderLineTax;
import com.kritik.POS.order.entity.OrderTaxContext;
import com.kritik.POS.order.entity.OrderTaxSummary;
import com.kritik.POS.order.entity.SaleItem;
import com.kritik.POS.order.model.request.OrderTaxContextRequest;
import com.kritik.POS.tax.entity.TaxRegistration;
import com.kritik.POS.tax.model.AppliedTaxComponent;
import com.kritik.POS.tax.model.TaxBuyerContext;
import com.kritik.POS.tax.model.TaxComputationResult;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import com.kritik.POS.tax.service.TaxService;
import com.kritik.POS.tax.util.MoneyUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final TaxService taxService;

    public void applyPricing(Order order,
                             Long restaurantId,
                             List<LinePricingPlan> plans,
                             OrderTaxContextRequest request) {
        List<TaxableChargeComponent> components = new ArrayList<>();
        Map<String, LinePricingPlan> lineByReference = new HashMap<>();
        BigDecimal subtotalAmount = MoneyUtils.zero();
        BigDecimal discountAmount = MoneyUtils.zero();

        for (LinePricingPlan plan : plans) {
            subtotalAmount = MoneyUtils.add(subtotalAmount, plan.lineSubtotalAmount());
            discountAmount = MoneyUtils.add(discountAmount, plan.lineDiscountAmount());
            for (TaxableChargeComponent component : plan.components()) {
                components.add(component);
                lineByReference.put(component.referenceKey(), plan);
            }
        }

        TaxComputationResult computation = taxService.computeOrderTaxes(
                restaurantId,
                components,
                toBuyerContext(request)
        );

        Map<LinePricingPlan, BigDecimal> grossByLine = sumComponentsByLine(plans);
        Map<LinePricingPlan, BigDecimal> inclusiveTaxByLine = new HashMap<>();
        Map<LinePricingPlan, BigDecimal> exclusiveTaxByLine = new HashMap<>();
        Map<LinePricingPlan, BigDecimal> allTaxByLine = new HashMap<>();
        List<OrderLineTax> lineTaxes = new ArrayList<>();

        for (AppliedTaxComponent appliedTax : computation.appliedTaxes()) {
            LinePricingPlan linePlan = lineByReference.get(appliedTax.referenceKey());
            if (linePlan == null) {
                continue;
            }
            if (appliedTax.calculationMode().name().equals("INCLUSIVE")) {
                inclusiveTaxByLine.merge(linePlan, appliedTax.taxAmount(), MoneyUtils::add);
            } else {
                exclusiveTaxByLine.merge(linePlan, appliedTax.taxAmount(), MoneyUtils::add);
            }
            allTaxByLine.merge(linePlan, appliedTax.taxAmount(), MoneyUtils::add);
            lineTaxes.add(toOrderLineTax(order, linePlan, appliedTax));
        }

        for (LinePricingPlan plan : plans) {
            BigDecimal gross = grossByLine.getOrDefault(plan, MoneyUtils.zero());
            BigDecimal inclusiveTax = inclusiveTaxByLine.getOrDefault(plan, MoneyUtils.zero());
            BigDecimal exclusiveTax = exclusiveTaxByLine.getOrDefault(plan, MoneyUtils.zero());
            BigDecimal totalTax = allTaxByLine.getOrDefault(plan, MoneyUtils.zero());
            BigDecimal lineTaxable = MoneyUtils.subtract(gross, inclusiveTax);
            BigDecimal lineTotal = MoneyUtils.add(gross, exclusiveTax);

            if (plan.saleItem() != null) {
                SaleItem saleItem = plan.saleItem();
                saleItem.setTaxClassCodeSnapshot(plan.taxClassCodeSnapshot());
                saleItem.setPriceIncludesTax(plan.priceIncludesTax());
                saleItem.setUnitListAmount(plan.unitListAmount());
                saleItem.setUnitDiscountAmount(plan.unitDiscountAmount());
                saleItem.setLineSubtotalAmount(plan.lineSubtotalAmount());
                saleItem.setLineDiscountAmount(plan.lineDiscountAmount());
                saleItem.setLineTaxableAmount(lineTaxable);
                saleItem.setLineTaxAmount(totalTax);
                saleItem.setLineTotalAmount(lineTotal);
                saleItem.setUnitTaxableAmount(divide(lineTaxable, saleItem.getAmount()));
                saleItem.setUnitTaxAmount(divide(totalTax, saleItem.getAmount()));
                saleItem.setUnitTotalAmount(divide(lineTotal, saleItem.getAmount()));
                saleItem.setSaleItemPrice(saleItem.getUnitTotalAmount());
            } else if (plan.configuredSaleItem() != null) {
                ConfiguredSaleItem configuredSaleItem = plan.configuredSaleItem();
                configuredSaleItem.setTaxClassCodeSnapshot(plan.taxClassCodeSnapshot());
                configuredSaleItem.setPriceIncludesTax(plan.priceIncludesTax());
                configuredSaleItem.setUnitListAmount(plan.unitListAmount());
                configuredSaleItem.setUnitDiscountAmount(plan.unitDiscountAmount());
                configuredSaleItem.setLineSubtotalAmount(plan.lineSubtotalAmount());
                configuredSaleItem.setLineDiscountAmount(plan.lineDiscountAmount());
                configuredSaleItem.setLineTaxableAmount(lineTaxable);
                configuredSaleItem.setLineTaxAmount(totalTax);
                configuredSaleItem.setLineTotalAmount(lineTotal);
                configuredSaleItem.setUnitTaxableAmount(divide(lineTaxable, configuredSaleItem.getAmount()));
                configuredSaleItem.setUnitTaxAmount(divide(totalTax, configuredSaleItem.getAmount()));
                configuredSaleItem.setUnitTotalAmount(divide(lineTotal, configuredSaleItem.getAmount()));
                configuredSaleItem.setUnitPrice(configuredSaleItem.getUnitTotalAmount());
            }
        }

        replaceOrderLineTaxes(order, lineTaxes);
        replaceOrderTaxSummaries(order, buildSummaries(order, computation.appliedTaxes()));
        upsertTaxContext(order, restaurantId, request);

        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTaxableAmount(computation.taxableAmount());
        order.setTaxAmount(computation.taxAmount());
        order.setFeeAmount(computation.feeAmount());
        order.setRoundingAmount(MoneyUtils.zero());
        order.setGrandTotal(computation.grandTotal());
        order.setTotalPrice(computation.grandTotal());
        if (order.getOrderTaxes() != null) {
            order.getOrderTaxes().clear();
        }
    }

    private Map<LinePricingPlan, BigDecimal> sumComponentsByLine(List<LinePricingPlan> plans) {
        Map<LinePricingPlan, BigDecimal> grossByLine = new HashMap<>();
        for (LinePricingPlan plan : plans) {
            BigDecimal gross = MoneyUtils.zero();
            for (TaxableChargeComponent component : plan.components()) {
                gross = MoneyUtils.add(gross, component.taxableAmount());
            }
            grossByLine.put(plan, gross);
        }
        return grossByLine;
    }

    private List<OrderTaxSummary> buildSummaries(Order order, List<AppliedTaxComponent> appliedTaxes) {
        Map<String, OrderTaxSummary> summaryByCode = new LinkedHashMap<>();
        for (AppliedTaxComponent appliedTax : appliedTaxes) {
            OrderTaxSummary summary = summaryByCode.computeIfAbsent(
                    appliedTax.taxDefinitionCode(),
                    ignored -> {
                        OrderTaxSummary orderTaxSummary = new OrderTaxSummary();
                        orderTaxSummary.setOrder(order);
                        orderTaxSummary.setTaxDefinitionCode(appliedTax.taxDefinitionCode());
                        orderTaxSummary.setTaxDisplayName(appliedTax.taxDisplayName());
                        orderTaxSummary.setTaxableBaseAmount(MoneyUtils.zero());
                        orderTaxSummary.setTaxAmount(MoneyUtils.zero());
                        orderTaxSummary.setCurrencyCode(appliedTax.currencyCode());
                        return orderTaxSummary;
                    }
            );
            summary.setTaxableBaseAmount(MoneyUtils.add(summary.getTaxableBaseAmount(), appliedTax.taxableBaseAmount()));
            summary.setTaxAmount(MoneyUtils.add(summary.getTaxAmount(), appliedTax.taxAmount()));
        }
        return new ArrayList<>(summaryByCode.values());
    }

    private OrderLineTax toOrderLineTax(Order order, LinePricingPlan plan, AppliedTaxComponent appliedTax) {
        OrderLineTax entity = new OrderLineTax();
        entity.setOrder(order);
        entity.setReferenceKey(appliedTax.referenceKey());
        entity.setTaxDefinitionCode(appliedTax.taxDefinitionCode());
        entity.setTaxDisplayName(appliedTax.taxDisplayName());
        entity.setValueType(appliedTax.valueType());
        entity.setRateOrAmount(appliedTax.rateOrAmount());
        entity.setCalculationMode(appliedTax.calculationMode());
        entity.setCompoundMode(appliedTax.compoundMode());
        entity.setSequenceNo(appliedTax.sequenceNo());
        entity.setTaxableBaseAmount(appliedTax.taxableBaseAmount());
        entity.setTaxAmount(appliedTax.taxAmount());
        entity.setCurrencyCode(appliedTax.currencyCode());
        entity.setJurisdictionCountryCode(appliedTax.jurisdictionCountryCode());
        entity.setJurisdictionRegionCode(appliedTax.jurisdictionRegionCode());
        entity.setSaleItem(plan.saleItem());
        entity.setConfiguredSaleItem(plan.configuredSaleItem());
        return entity;
    }

    private void replaceOrderTaxSummaries(Order order, List<OrderTaxSummary> summaries) {
        List<OrderTaxSummary> managed = order.getOrderTaxSummaries();
        managed.clear();
        managed.addAll(summaries);
    }

    private void replaceOrderLineTaxes(Order order, List<OrderLineTax> lineTaxes) {
        List<OrderLineTax> managed = order.getOrderLineTaxes();
        managed.clear();
        managed.addAll(lineTaxes);
    }

    private void upsertTaxContext(Order order, Long restaurantId, OrderTaxContextRequest request) {
        OrderTaxContext context = order.getOrderTaxContext();
        if (context == null) {
            context = new OrderTaxContext();
            context.setOrder(order);
            order.setOrderTaxContext(context);
        }
        TaxRegistration registration = taxService.getDefaultTaxRegistration(restaurantId);
        if (registration != null) {
            context.setSellerTaxRegistrationId(registration.getId());
            context.setSellerRegistrationNumberSnapshot(registration.getRegistrationNumber());
            context.setSellerCountryCode(registration.getCountryCode());
            context.setSellerRegionCode(registration.getRegionCode());
        } else {
            context.setSellerTaxRegistrationId(null);
            context.setSellerRegistrationNumberSnapshot(null);
            context.setSellerCountryCode(null);
            context.setSellerRegionCode(null);
        }
        if (request == null) {
            context.setBuyerName(null);
            context.setBuyerTaxId(null);
            context.setBuyerTaxCategory(null);
            context.setBuyerCountryCode(null);
            context.setBuyerRegionCode(null);
            context.setBillingAddressJson(null);
            context.setPlaceOfSupplyCountryCode(null);
            context.setPlaceOfSupplyRegionCode(null);
            return;
        }
        context.setBuyerName(request.buyerName());
        context.setBuyerTaxId(request.buyerTaxId());
        context.setBuyerTaxCategory(request.buyerTaxCategory());
        context.setBuyerCountryCode(request.buyerCountryCode());
        context.setBuyerRegionCode(request.buyerRegionCode());
        context.setBillingAddressJson(request.billingAddressJson());
        context.setPlaceOfSupplyCountryCode(request.placeOfSupplyCountryCode());
        context.setPlaceOfSupplyRegionCode(request.placeOfSupplyRegionCode());
    }

    private TaxBuyerContext toBuyerContext(OrderTaxContextRequest request) {
        if (request == null) {
            return new TaxBuyerContext(null, null, null, null, null, null, null, null);
        }
        return new TaxBuyerContext(
                request.buyerName(),
                request.buyerTaxId(),
                request.buyerTaxCategory(),
                request.buyerCountryCode(),
                request.buyerRegionCode(),
                request.billingAddressJson(),
                request.placeOfSupplyCountryCode(),
                request.placeOfSupplyRegionCode()
        );
    }

    private BigDecimal divide(BigDecimal value, Integer quantity) {
        if (quantity == null || quantity == 0) {
            return MoneyUtils.zero();
        }
        return value.divide(BigDecimal.valueOf(quantity), MoneyUtils.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public record LinePricingPlan(
            String lineKey,
            String taxClassCodeSnapshot,
            boolean priceIncludesTax,
            BigDecimal unitListAmount,
            BigDecimal unitDiscountAmount,
            BigDecimal lineSubtotalAmount,
            BigDecimal lineDiscountAmount,
            List<TaxableChargeComponent> components,
            SaleItem saleItem,
            ConfiguredSaleItem configuredSaleItem
    ) {
        public static LinePricingPlan forSaleItem(String lineKey,
                                                  String taxClassCodeSnapshot,
                                                  boolean priceIncludesTax,
                                                  BigDecimal unitListAmount,
                                                  BigDecimal unitDiscountAmount,
                                                  BigDecimal lineSubtotalAmount,
                                                  BigDecimal lineDiscountAmount,
                                                  List<TaxableChargeComponent> components,
                                                  SaleItem saleItem) {
            return new LinePricingPlan(
                    lineKey,
                    taxClassCodeSnapshot,
                    priceIncludesTax,
                    unitListAmount,
                    unitDiscountAmount,
                    lineSubtotalAmount,
                    lineDiscountAmount,
                    components,
                    saleItem,
                    null
            );
        }

        public static LinePricingPlan forConfiguredSaleItem(String lineKey,
                                                            String taxClassCodeSnapshot,
                                                            boolean priceIncludesTax,
                                                            BigDecimal unitListAmount,
                                                            BigDecimal unitDiscountAmount,
                                                            BigDecimal lineSubtotalAmount,
                                                            BigDecimal lineDiscountAmount,
                                                            List<TaxableChargeComponent> components,
                                                            ConfiguredSaleItem configuredSaleItem) {
            return new LinePricingPlan(
                    lineKey,
                    taxClassCodeSnapshot,
                    priceIncludesTax,
                    unitListAmount,
                    unitDiscountAmount,
                    lineSubtotalAmount,
                    lineDiscountAmount,
                    components,
                    null,
                    configuredSaleItem
            );
        }
    }
}
