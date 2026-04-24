package com.kritik.POS.tax.dto;

import java.time.LocalDate;
import java.util.List;

public record TaxCatalogSeedCountryResult(
        String countryCode,
        String datasetCode,
        LocalDate effectiveFrom,
        int taxClassesCreated,
        int taxClassesUpdated,
        int taxDefinitionsCreated,
        int taxDefinitionsUpdated,
        int taxRulesCreated,
        int taxRulesUpdated,
        boolean defaultRegistrationCreated,
        String defaultRegistrationNumber,
        List<String> warnings
) {
}
