package com.kritik.POS.tax.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.repository.RestaurantRepository;
import com.kritik.POS.security.service.TenantAccessService;
import com.kritik.POS.tax.dto.TaxCatalogSeedRequest;
import com.kritik.POS.tax.dto.TaxCatalogSeedResponse;
import com.kritik.POS.tax.entity.TaxClass;
import com.kritik.POS.tax.entity.TaxDefinition;
import com.kritik.POS.tax.entity.TaxRegistration;
import com.kritik.POS.tax.entity.TaxRule;
import com.kritik.POS.tax.entity.enums.TaxCalculationMode;
import com.kritik.POS.tax.entity.enums.TaxCompoundMode;
import com.kritik.POS.tax.entity.enums.TaxDefinitionKind;
import com.kritik.POS.tax.entity.enums.TaxSupplyScope;
import com.kritik.POS.tax.entity.enums.TaxValueType;
import com.kritik.POS.tax.model.TaxBuyerContext;
import com.kritik.POS.tax.model.TaxComputationResult;
import com.kritik.POS.tax.model.TaxableChargeComponent;
import com.kritik.POS.tax.repository.TaxClassRepository;
import com.kritik.POS.tax.repository.TaxDefinitionRepository;
import com.kritik.POS.tax.repository.TaxRegistrationRepository;
import com.kritik.POS.tax.repository.TaxRuleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxServiceImplTest {

    @Mock
    private TaxClassRepository taxClassRepository;

    @Mock
    private TaxDefinitionRepository taxDefinitionRepository;

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @Mock
    private TaxRegistrationRepository taxRegistrationRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private TaxServiceImpl taxService;

    @Test
    void seedTaxCatalogCreatesIndiaDatasetForRestaurant() {
        Long restaurantId = 11L;
        Restaurant restaurant = buildRestaurant(restaurantId, "Maharashtra", "27ABCDE1234F1Z5");
        AtomicLong classIds = new AtomicLong(1);
        AtomicLong definitionIds = new AtomicLong(100);
        AtomicLong ruleIds = new AtomicLong(1000);
        AtomicLong registrationIds = new AtomicLong(10000);

        when(tenantAccessService.resolveManageableRestaurantId(restaurantId)).thenReturn(restaurantId);
        when(restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId)).thenReturn(Optional.of(restaurant));
        when(taxRegistrationRepository.findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurantId))
                .thenReturn(Optional.empty());
        when(taxClassRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId)).thenReturn(List.of());
        when(taxDefinitionRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId)).thenReturn(List.of());
        when(taxRuleRepository.findAllByRestaurantIdAndIsDeletedFalse(restaurantId)).thenReturn(List.of());
        when(taxClassRepository.save(any(TaxClass.class))).thenAnswer(invocation -> {
            TaxClass entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(classIds.getAndIncrement());
            }
            return entity;
        });
        when(taxDefinitionRepository.save(any(TaxDefinition.class))).thenAnswer(invocation -> {
            TaxDefinition entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(definitionIds.getAndIncrement());
            }
            return entity;
        });
        when(taxRuleRepository.save(any(TaxRule.class))).thenAnswer(invocation -> {
            TaxRule entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(ruleIds.getAndIncrement());
            }
            return entity;
        });
        when(taxRegistrationRepository.save(any(TaxRegistration.class))).thenAnswer(invocation -> {
            TaxRegistration entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(registrationIds.getAndIncrement());
            }
            return entity;
        });

        TaxCatalogSeedResponse response = taxService.seedTaxCatalog(
                new TaxCatalogSeedRequest(restaurantId, List.of("india"), LocalDate.of(2026, 4, 1), true)
        );

        assertThat(response.datasets()).hasSize(1);
        assertThat(response.datasets().get(0).countryCode()).isEqualTo("IN");
        assertThat(response.datasets().get(0).taxClassesCreated()).isEqualTo(8);
        assertThat(response.datasets().get(0).taxDefinitionsCreated()).isEqualTo(15);
        assertThat(response.datasets().get(0).taxRulesCreated()).isEqualTo(18);
        assertThat(response.datasets().get(0).defaultRegistrationCreated()).isTrue();
        assertThat(response.datasets().get(0).defaultRegistrationNumber()).isEqualTo("27ABCDE1234F1Z5");
    }

    @Test
    void computeOrderTaxesUsesCgstAndSgstForIntrastateIndiaSupply() {
        Long restaurantId = 22L;
        Long taxClassId = 501L;
        TaxClass taxClass = new TaxClass();
        taxClass.setId(taxClassId);
        taxClass.setRestaurantId(restaurantId);
        taxClass.setCode("STANDARD");
        taxClass.setExempt(false);
        taxClass.setDeleted(false);

        TaxDefinition cgst = taxDefinition(101L, "IN_RESTAURANT_CGST_2_5", BigDecimal.valueOf(2.5));
        TaxDefinition sgst = taxDefinition(102L, "IN_RESTAURANT_SGST_2_5", BigDecimal.valueOf(2.5));
        TaxDefinition igst = taxDefinition(103L, "IN_RESTAURANT_IGST_5", BigDecimal.valueOf(5));

        when(tenantAccessService.resolveAccessibleRestaurantId(restaurantId)).thenReturn(restaurantId);
        when(taxRegistrationRepository.findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurantId))
                .thenReturn(Optional.of(registration(restaurantId, "IN", "27")));
        when(restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId))
                .thenReturn(Optional.of(buildRestaurant(restaurantId, "Maharashtra", "27ABCDE1234F1Z5")));
        when(taxRuleRepository.findActiveForRestaurant(restaurantId)).thenReturn(List.of(
                taxRule(restaurantId, taxClassId, cgst.getId(), "IN", "27", TaxSupplyScope.INTRA_STATE, 1),
                taxRule(restaurantId, taxClassId, sgst.getId(), "IN", "27", TaxSupplyScope.INTRA_STATE, 2),
                taxRule(restaurantId, taxClassId, igst.getId(), "IN", null, TaxSupplyScope.INTER_STATE, 1)
        ));
        when(taxDefinitionRepository.findAllByIdIn(any())).thenReturn(List.of(cgst, sgst, igst));
        when(taxClassRepository.findAllById(any())).thenReturn(List.of(taxClass));

        TaxComputationResult result = taxService.computeOrderTaxes(
                restaurantId,
                List.of(new TaxableChargeComponent("line-1", "STANDARD", taxClassId, BigDecimal.valueOf(100), false)),
                new TaxBuyerContext(null, null, null, "IN", "Maharashtra", null, "IN", "27")
        );

        assertThat(result.taxAmount()).isEqualByComparingTo("5.00");
        assertThat(result.appliedTaxes()).hasSize(2);
        assertThat(result.appliedTaxes()).extracting(component -> component.taxDefinitionCode())
                .containsExactly("IN_RESTAURANT_CGST_2_5", "IN_RESTAURANT_SGST_2_5");
    }

    @Test
    void computeOrderTaxesUsesIgstForInterstateIndiaSupply() {
        Long restaurantId = 33L;
        Long taxClassId = 601L;
        TaxClass taxClass = new TaxClass();
        taxClass.setId(taxClassId);
        taxClass.setRestaurantId(restaurantId);
        taxClass.setCode("STANDARD");
        taxClass.setExempt(false);
        taxClass.setDeleted(false);

        TaxDefinition cgst = taxDefinition(201L, "IN_RESTAURANT_CGST_2_5", BigDecimal.valueOf(2.5));
        TaxDefinition sgst = taxDefinition(202L, "IN_RESTAURANT_SGST_2_5", BigDecimal.valueOf(2.5));
        TaxDefinition igst = taxDefinition(203L, "IN_RESTAURANT_IGST_5", BigDecimal.valueOf(5));

        when(tenantAccessService.resolveAccessibleRestaurantId(restaurantId)).thenReturn(restaurantId);
        when(taxRegistrationRepository.findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(restaurantId))
                .thenReturn(Optional.of(registration(restaurantId, "IN", "27")));
        when(restaurantRepository.findByRestaurantIdAndIsDeletedFalse(restaurantId))
                .thenReturn(Optional.of(buildRestaurant(restaurantId, "Maharashtra", "27ABCDE1234F1Z5")));
        when(taxRuleRepository.findActiveForRestaurant(restaurantId)).thenReturn(List.of(
                taxRule(restaurantId, taxClassId, cgst.getId(), "IN", "27", TaxSupplyScope.INTRA_STATE, 1),
                taxRule(restaurantId, taxClassId, sgst.getId(), "IN", "27", TaxSupplyScope.INTRA_STATE, 2),
                taxRule(restaurantId, taxClassId, igst.getId(), "IN", null, TaxSupplyScope.INTER_STATE, 1)
        ));
        when(taxDefinitionRepository.findAllByIdIn(any())).thenReturn(List.of(cgst, sgst, igst));
        when(taxClassRepository.findAllById(any())).thenReturn(List.of(taxClass));

        TaxComputationResult result = taxService.computeOrderTaxes(
                restaurantId,
                List.of(new TaxableChargeComponent("line-1", "STANDARD", taxClassId, BigDecimal.valueOf(100), false)),
                new TaxBuyerContext(null, null, null, "IN", "Karnataka", null, "IN", "29")
        );

        assertThat(result.taxAmount()).isEqualByComparingTo("5.00");
        assertThat(result.appliedTaxes()).hasSize(1);
        assertThat(result.appliedTaxes().get(0).taxDefinitionCode()).isEqualTo("IN_RESTAURANT_IGST_5");
    }

    private Restaurant buildRestaurant(Long restaurantId, String state, String gstNumber) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(restaurantId);
        restaurant.setName("Test Restaurant");
        restaurant.setCode("TR-" + restaurantId);
        restaurant.setState(state);
        restaurant.setCountry("India");
        restaurant.setGstNumber(gstNumber);
        restaurant.setAddressLine1("Main Street");
        restaurant.setCity("Mumbai");
        restaurant.setPincode("400001");
        return restaurant;
    }

    private TaxDefinition taxDefinition(Long id, String code, BigDecimal rate) {
        TaxDefinition definition = new TaxDefinition();
        definition.setId(id);
        definition.setCode(code);
        definition.setDisplayName(code);
        definition.setKind(TaxDefinitionKind.TAX);
        definition.setValueType(TaxValueType.PERCENT);
        definition.setDefaultValue(rate);
        definition.setCurrencyCode("INR");
        definition.setActive(true);
        definition.setDeleted(false);
        return definition;
    }

    private TaxRegistration registration(Long restaurantId, String countryCode, String regionCode) {
        TaxRegistration registration = new TaxRegistration();
        registration.setRestaurantId(restaurantId);
        registration.setCountryCode(countryCode);
        registration.setRegionCode(regionCode);
        registration.setRegistrationNumber("27ABCDE1234F1Z5");
        registration.setDefault(true);
        registration.setActive(true);
        return registration;
    }

    private TaxRule taxRule(Long restaurantId,
                            Long taxClassId,
                            Long definitionId,
                            String countryCode,
                            String regionCode,
                            TaxSupplyScope supplyScope,
                            int sequenceNo) {
        TaxRule rule = new TaxRule();
        rule.setRestaurantId(restaurantId);
        rule.setTaxClassId(taxClassId);
        rule.setTaxDefinitionId(definitionId);
        rule.setCalculationMode(TaxCalculationMode.EXCLUSIVE);
        rule.setCompoundMode(TaxCompoundMode.BASE_ONLY);
        rule.setCountryCode(countryCode);
        rule.setRegionCode(regionCode);
        rule.setSupplyScope(supplyScope);
        rule.setSequenceNo(sequenceNo);
        rule.setPriority(100);
        rule.setActive(true);
        rule.setDeleted(false);
        return rule;
    }
}
