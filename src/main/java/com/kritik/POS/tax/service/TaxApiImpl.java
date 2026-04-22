package com.kritik.POS.tax.service;

import com.kritik.POS.tax.api.AppliedTaxComponent;
import com.kritik.POS.tax.api.TaxApi;
import com.kritik.POS.tax.api.TaxBuyerContext;
import com.kritik.POS.tax.api.TaxCalculationMode;
import com.kritik.POS.tax.api.TaxClassSnapshot;
import com.kritik.POS.tax.api.TaxComputationResult;
import com.kritik.POS.tax.api.TaxCompoundMode;
import com.kritik.POS.tax.api.TaxDefinitionKind;
import com.kritik.POS.tax.api.TaxRegistrationSnapshot;
import com.kritik.POS.tax.api.TaxValueType;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.entity.TaxRegistration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaxApiImpl implements TaxApi {

    private final TaxService taxService;

    @Override
    public TaxClassSnapshot resolveTaxClass(Long restaurantId, Long taxClassId) {
        TaxClass taxClass = taxService.resolveTaxClass(restaurantId, taxClassId);
        return new TaxClassSnapshot(
                taxClass.getId(),
                taxClass.getRestaurantId(),
                taxClass.getCode(),
                taxClass.isExempt()
        );
    }

    @Override
    public TaxRegistrationSnapshot getDefaultTaxRegistration(Long restaurantId) {
        TaxRegistration registration = taxService.getDefaultTaxRegistration(restaurantId);
        if (registration == null) {
            return null;
        }
        return new TaxRegistrationSnapshot(
                registration.getId(),
                registration.getRegistrationNumber(),
                registration.getCountryCode(),
                registration.getRegionCode()
        );
    }

    @Override
    public TaxComputationResult computeOrderTaxes(Long restaurantId,
                                                  List<com.kritik.POS.tax.api.TaxableChargeComponent> components,
                                                  TaxBuyerContext buyerContext) {
        com.kritik.POS.tax.model.TaxComputationResult result = taxService.computeOrderTaxes(
                restaurantId,
                components.stream()
                        .map(component -> new com.kritik.POS.tax.model.TaxableChargeComponent(
                                component.referenceKey(),
                                component.taxClassCode(),
                                component.taxClassId(),
                                component.taxableAmount(),
                                component.priceIncludesTax()
                        ))
                        .toList(),
                new com.kritik.POS.tax.model.TaxBuyerContext(
                        buyerContext.buyerName(),
                        buyerContext.buyerTaxId(),
                        buyerContext.buyerTaxCategory(),
                        buyerContext.buyerCountryCode(),
                        buyerContext.buyerRegionCode(),
                        buyerContext.billingAddressJson(),
                        buyerContext.placeOfSupplyCountryCode(),
                        buyerContext.placeOfSupplyRegionCode()
                )
        );

        return new TaxComputationResult(
                result.subtotalAmount(),
                result.discountAmount(),
                result.taxableAmount(),
                result.taxAmount(),
                result.feeAmount(),
                result.grandTotal(),
                result.appliedTaxes().stream()
                        .map(appliedTax -> new AppliedTaxComponent(
                                appliedTax.referenceKey(),
                                appliedTax.taxDefinitionCode(),
                                appliedTax.taxDisplayName(),
                                TaxDefinitionKind.valueOf(appliedTax.kind().name()),
                                TaxValueType.valueOf(appliedTax.valueType().name()),
                                appliedTax.rateOrAmount(),
                                TaxCalculationMode.valueOf(appliedTax.calculationMode().name()),
                                TaxCompoundMode.valueOf(appliedTax.compoundMode().name()),
                                appliedTax.sequenceNo(),
                                appliedTax.taxableBaseAmount(),
                                appliedTax.taxAmount(),
                                appliedTax.currencyCode(),
                                appliedTax.jurisdictionCountryCode(),
                                appliedTax.jurisdictionRegionCode()
                        ))
                        .toList()
        );
    }
}
