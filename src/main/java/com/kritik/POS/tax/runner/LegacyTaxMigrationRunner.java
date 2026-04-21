package com.kritik.POS.tax.runner;

import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.entity.TaxDefinition;
import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.entity.TaxRegistration;
import com.kritik.POS.tax.entity.TaxRule;
import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import com.kritik.POS.tax.repository.TaxClassRepository;
import com.kritik.POS.tax.repository.TaxDefinitionRepository;
import com.kritik.POS.tax.repository.TaxRateRepository;
import com.kritik.POS.tax.repository.TaxRegistrationRepository;
import com.kritik.POS.tax.repository.TaxRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyTaxMigrationRunner implements CommandLineRunner {

    private static final String DEFAULT_TAX_CLASS_CODE = "STANDARD";

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final TaxRateRepository taxRateRepository;
    private final TaxClassRepository taxClassRepository;
    private final TaxDefinitionRepository taxDefinitionRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final TaxRegistrationRepository taxRegistrationRepository;

    @Override
    public void run(String... args) {
        List<Restaurant> restaurants = restaurantRepository.findAll().stream()
                .filter(restaurant -> !restaurant.isDeleted())
                .toList();
        if (restaurants.isEmpty()) {
            return;
        }

        for (Restaurant restaurant : restaurants) {
            migrateRestaurant(restaurant);
        }
    }

    private void migrateRestaurant(Restaurant restaurant) {
        TaxClass defaultTaxClass = taxClassRepository
                .findByRestaurantIdAndCodeAndIsDeletedFalse(restaurant.getRestaurantId(), DEFAULT_TAX_CLASS_CODE)
                .orElseGet(() -> createDefaultTaxClass(restaurant));

        backfillMenuItems(restaurant.getRestaurantId(), defaultTaxClass.getId());
        backfillTaxRegistration(restaurant);
        backfillLegacyTaxRates(restaurant.getRestaurantId(), defaultTaxClass);
    }

    private TaxClass createDefaultTaxClass(Restaurant restaurant) {
        TaxClass taxClass = new TaxClass();
        taxClass.setRestaurantId(restaurant.getRestaurantId());
        taxClass.setCode(DEFAULT_TAX_CLASS_CODE);
        taxClass.setName("Standard Taxable");
        taxClass.setDescription("Default taxable class created during legacy tax migration");
        taxClass.setActive(true);
        taxClass.setDeleted(false);
        return taxClassRepository.save(taxClass);
    }

    private void backfillMenuItems(Long restaurantId, Long taxClassId) {
        List<MenuItem> pendingItems = menuItemRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId).stream()
                .filter(menuItem -> menuItem.getTaxClassId() == null)
                .toList();
        if (pendingItems.isEmpty()) {
            return;
        }

        pendingItems.forEach(menuItem -> menuItem.setTaxClassId(taxClassId));
        menuItemRepository.saveAll(pendingItems);
    }

    private void backfillTaxRegistration(Restaurant restaurant) {
        if (!StringUtils.hasText(restaurant.getGstNumber())) {
            return;
        }
        if (taxRegistrationRepository.findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(
                restaurant.getRestaurantId()).isPresent()) {
            return;
        }

        TaxRegistration registration = new TaxRegistration();
        registration.setRestaurantId(restaurant.getRestaurantId());
        registration.setSchemeCode("GST");
        registration.setRegistrationNumber(restaurant.getGstNumber().trim());
        registration.setLegalName(restaurant.getName());
        registration.setCountryCode(trimToNull(restaurant.getCountry()));
        registration.setRegionCode(trimToNull(restaurant.getState()));
        registration.setPlaceOfBusiness(buildPlaceOfBusiness(restaurant));
        registration.setDefault(true);
        registration.setActive(restaurant.isActive());
        taxRegistrationRepository.save(registration);
    }

    private void backfillLegacyTaxRates(Long restaurantId, TaxClass defaultTaxClass) {
        List<TaxRate> legacyRates = taxRateRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        if (legacyRates.isEmpty()) {
            return;
        }

        List<TaxRule> existingRules = taxRuleRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId);
        Set<String> existingRuleKeys = new HashSet<>();
        for (TaxRule existingRule : existingRules) {
            existingRuleKeys.add(ruleKey(existingRule.getTaxDefinitionId(), existingRule.getTaxClassId()));
        }

        for (TaxRate legacyRate : legacyRates) {
            TaxDefinition definition = resolveDefinition(legacyRate);
            String defaultRuleKey = ruleKey(definition.getId(), defaultTaxClass.getId());
            if (existingRuleKeys.contains(defaultRuleKey)) {
                continue;
            }

            TaxRule rule = new TaxRule();
            rule.setRestaurantId(restaurantId);
            rule.setTaxDefinitionId(definition.getId());
            rule.setTaxClassId(defaultTaxClass.getId());
            rule.setCalculationMode(TaxCalculationMode.EXCLUSIVE);
            rule.setCompoundMode(TaxCompoundMode.BASE_ONLY);
            rule.setSequenceNo(1);
            rule.setPriority(0);
            rule.setActive(legacyRate.isActive());
            rule.setDeleted(false);
            copyAuditTimestamps(rule, legacyRate.getCreatedAt(), legacyRate.getUpdatedAt());
            taxRuleRepository.save(rule);
            existingRuleKeys.add(defaultRuleKey);
        }
    }

    private TaxDefinition resolveDefinition(TaxRate legacyRate) {
        String definitionCode = buildBaseDefinitionCode(legacyRate);
        return taxDefinitionRepository
                .findByRestaurantIdAndCodeAndIsDeletedFalse(legacyRate.getRestaurantId(), definitionCode)
                .orElseGet(() -> createDefinition(legacyRate, resolveAvailableDefinitionCode(legacyRate.getRestaurantId(), definitionCode)));
    }

    private TaxDefinition createDefinition(TaxRate legacyRate, String definitionCode) {
        TaxDefinition definition = new TaxDefinition();
        definition.setRestaurantId(legacyRate.getRestaurantId());
        definition.setCode(definitionCode);
        definition.setDisplayName(legacyRate.getTaxName().trim());
        definition.setKind(TaxDefinitionKind.TAX);
        definition.setValueType(TaxValueType.PERCENT);
        definition.setDefaultValue(BigDecimal.valueOf(legacyRate.getTaxAmount()));
        definition.setCurrencyCode("INR");
        definition.setRecoverable(true);
        definition.setActive(legacyRate.isActive());
        definition.setDeleted(false);
        copyAuditTimestamps(definition, legacyRate.getCreatedAt(), legacyRate.getUpdatedAt());
        return taxDefinitionRepository.save(definition);
    }

    private void copyAuditTimestamps(TaxDefinition definition, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null) {
            definition.setCreatedAt(createdAt);
        }
        if (updatedAt != null) {
            definition.setUpdatedAt(updatedAt);
        }
    }

    private void copyAuditTimestamps(TaxRule rule, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (createdAt != null) {
            rule.setCreatedAt(createdAt);
        }
        if (updatedAt != null) {
            rule.setUpdatedAt(updatedAt);
        }
    }

    private String buildBaseDefinitionCode(TaxRate legacyRate) {
        String normalized = legacyRate.getTaxName() == null ? "" : legacyRate.getTaxName()
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        if (!StringUtils.hasText(normalized)) {
            return "LEGACY_TAX_" + legacyRate.getTaxId();
        }
        return normalized;
    }

    private String resolveAvailableDefinitionCode(Long restaurantId, String baseCode) {
        String candidate = baseCode;
        int suffix = 1;
        while (taxDefinitionRepository.findByRestaurantIdAndCodeAndIsDeletedFalse(restaurantId, candidate).isPresent()) {
            candidate = baseCode + "_" + suffix++;
        }
        return candidate;
    }

    private String buildPlaceOfBusiness(Restaurant restaurant) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, restaurant.getAddressLine1());
        appendPart(builder, restaurant.getAddressLine2());
        appendPart(builder, restaurant.getCity());
        appendPart(builder, restaurant.getState());
        appendPart(builder, restaurant.getPincode());
        return trimToNull(builder.toString());
    }

    private void appendPart(StringBuilder builder, String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(trimmed);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String ruleKey(Long taxDefinitionId, Long taxClassId) {
        return taxDefinitionId + ":" + taxClassId;
    }
}
