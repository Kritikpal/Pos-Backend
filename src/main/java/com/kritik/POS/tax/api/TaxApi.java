package com.kritik.POS.tax.api;

import java.util.List;

public interface TaxApi {

    TaxClassSnapshot resolveTaxClass(Long restaurantId, Long taxClassId);

    TaxRegistrationSnapshot getDefaultTaxRegistration(Long restaurantId);

    TaxComputationResult computeOrderTaxes(Long restaurantId,
                                           List<TaxableChargeComponent> components,
                                           TaxBuyerContext buyerContext);
}
