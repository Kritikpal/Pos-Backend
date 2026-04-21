package com.kritik.POS.configuredmenu.service.Impl;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.models.request.ConfiguredMenuTemplateRequest;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuItemSearchDto;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuTemplateResponse;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.inventory.projection.RecipeMenuItemSearchProjection;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.ItemPrice;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguredMenuServiceImplTest {

    @Mock
    private ConfiguredMenuTemplateRepository configuredMenuTemplateRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private TenantAccessService tenantAccessService;

    @InjectMocks
    private ConfiguredMenuServiceImpl configuredMenuService;

    @Test
    void searchCandidateMenuItemsReturnsOnlyConfigurableMenusWhenRecipeBasedIsFalse() {
        List<Long> accessibleRestaurantIds = List.of(101L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(1L, 101L)).thenReturn(accessibleRestaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)).thenReturn(accessibleRestaurantIds);
        when(menuItemRepository.searchRecipeMenuItems(anyBoolean(), anyCollection(), anyCollection(), anyString(), any(PageRequest.class)))
                .thenReturn(List.of(projection(11L, null, "Burger Combo", "/images/burger.png")));

        List<ConfiguredMenuItemSearchDto> response = configuredMenuService.searchCandidateMenuItems(1L, 101L, "burger", false, 5);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(11L);

        assertCapturedTypes(List.of(MenuType.CONFIGURABLE));
    }

    @Test
    void searchCandidateMenuItemsReturnsRecipeBasedMenusWhenRecipeBasedIsTrue() {
        List<Long> accessibleRestaurantIds = List.of(101L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(1L, 101L)).thenReturn(accessibleRestaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)).thenReturn(accessibleRestaurantIds);
        when(menuItemRepository.searchRecipeMenuItems(anyBoolean(), anyCollection(), anyCollection(), anyString(), any(PageRequest.class)))
                .thenReturn(List.of(projection(22L, 88L, "Paneer Tikka", "/images/paneer.png")));

        List<ConfiguredMenuItemSearchDto> response = configuredMenuService.searchCandidateMenuItems(1L, 101L, "paneer", true, 5);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).recipeId()).isEqualTo(88L);

        assertCapturedTypes(List.of(MenuType.RECIPE, MenuType.PREPARED));
    }

    @Test
    void searchCandidateMenuItemsReturnsConfigurableAndRecipeBasedMenusByDefault() {
        List<Long> accessibleRestaurantIds = List.of(101L);

        when(tenantAccessService.resolveAccessibleRestaurantIds(1L, 101L)).thenReturn(accessibleRestaurantIds);
        when(tenantAccessService.isSuperAdmin()).thenReturn(false);
        when(tenantAccessService.queryRestaurantIds(accessibleRestaurantIds)).thenReturn(accessibleRestaurantIds);
        when(menuItemRepository.searchRecipeMenuItems(anyBoolean(), anyCollection(), anyCollection(), anyString(), any(PageRequest.class)))
                .thenReturn(List.of(projection(33L, 99L, "Meal Box", "/images/meal.png")));

        configuredMenuService.searchCandidateMenuItems(1L, 101L, "meal", null, 5);

        assertCapturedTypes(List.of(MenuType.CONFIGURABLE, MenuType.RECIPE, MenuType.PREPARED));
    }

    @Test
    void createTemplatePersistsSelectionSlotRules() {
        MenuItem parentMenuItem = configurableMenu(10L, 101L, 250.0);
        MenuItem roti = regularMenu(20L, 101L, "Roti");
        MenuItem naan = regularMenu(21L, 101L, "Naan");

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parentMenuItem);
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(tenantAccessService.resolveManageableRestaurantId(101L)).thenReturn(101L);
        when(configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(false);
        when(configuredMenuTemplateRepository.save(any(ConfiguredMenuTemplate.class))).thenAnswer(invocation -> {
            ConfiguredMenuTemplate template = invocation.getArgument(0);
            assertThat(template.getSlots().get(0).getMinSelections()).isEqualTo(1);
            assertThat(template.getSlots().get(0).getMaxSelections()).isEqualTo(2);
            assertThat(template.getSlots().get(0).getOptions())
                    .extracting(option -> option.getMinQuantity())
                    .containsExactly(4, null);
            template.setId(500L);
            template.getSlots().get(0).setId(600L);
            return template;
        });

        ConfiguredMenuTemplateResponse response = configuredMenuService.createTemplate(new ConfiguredMenuTemplateRequest(
                10L,
                true,
                List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                        "breads",
                                "Breads",
                                1,
                                2,
                                0,
                                true,
                                List.of(
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(20L, 0.0, 0, true, 4),
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(21L, 10.0, 1, false, null)
                        )
                ))
        ));

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.slots()).hasSize(1);
        assertThat(response.slots().get(0).minSelections()).isEqualTo(1);
        assertThat(response.slots().get(0).maxSelections()).isEqualTo(2);
        assertThat(response.slots().get(0).options())
                .extracting(ConfiguredMenuTemplateResponse.ConfiguredMenuOptionResponse::minQuantity)
                .containsExactly(4, null);
    }

    @Test
    void createTemplateSupportsExactOneAndMixableSelectionSlots() {
        MenuItem parentMenuItem = configurableMenu(10L, 101L, 250.0);
        MenuItem plainRice = regularMenu(30L, 101L, "Plain Rice");
        MenuItem jeeraRice = regularMenu(31L, 101L, "Jeera Rice");
        MenuItem roti = regularMenu(20L, 101L, "Roti");
        MenuItem naan = regularMenu(21L, 101L, "Naan");

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parentMenuItem);
        when(inventoryService.getAccessibleMenuItem(30L)).thenReturn(plainRice);
        when(inventoryService.getAccessibleMenuItem(31L)).thenReturn(jeeraRice);
        when(inventoryService.getAccessibleMenuItem(20L)).thenReturn(roti);
        when(inventoryService.getAccessibleMenuItem(21L)).thenReturn(naan);
        when(tenantAccessService.resolveManageableRestaurantId(101L)).thenReturn(101L);
        when(configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(false);
        when(configuredMenuTemplateRepository.save(any(ConfiguredMenuTemplate.class))).thenAnswer(invocation -> {
            ConfiguredMenuTemplate template = invocation.getArgument(0);
            assertThat(template.getSlots()).hasSize(2);
            assertThat(template.getSlots().get(0).getMinSelections()).isEqualTo(1);
            assertThat(template.getSlots().get(0).getMaxSelections()).isEqualTo(1);
            assertThat(template.getSlots().get(0).getOptions())
                    .extracting(option -> option.getMinQuantity())
                    .containsExactly(1, null);
            assertThat(template.getSlots().get(1).getMinSelections()).isEqualTo(1);
            assertThat(template.getSlots().get(1).getMaxSelections()).isEqualTo(2);
            assertThat(template.getSlots().get(1).getOptions())
                    .extracting(option -> option.getMinQuantity())
                    .containsExactly(4, null);
            template.setId(501L);
            template.getSlots().get(0).setId(601L);
            template.getSlots().get(1).setId(602L);
            return template;
        });

        ConfiguredMenuTemplateResponse response = configuredMenuService.createTemplate(new ConfiguredMenuTemplateRequest(
                10L,
                true,
                List.of(
                        new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                                "rice",
                                "Rice",
                                1,
                                1,
                                0,
                                true,
                                List.of(
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(30L, 0.0, 0, true, 1),
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(31L, 15.0, 1, false, null)
                                )
                        ),
                        new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                                "breads",
                                "Breads",
                                1,
                                2,
                                1,
                                true,
                                List.of(
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(20L, 10.0, 0, true, 4),
                                        new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(21L, 20.0, 1, false, null)
                                )
                        )
                )
        ));

        assertThat(response.id()).isEqualTo(501L);
        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().get(0).minSelections()).isEqualTo(1);
        assertThat(response.slots().get(0).maxSelections()).isEqualTo(1);
        assertThat(response.slots().get(0).options())
                .extracting(ConfiguredMenuTemplateResponse.ConfiguredMenuOptionResponse::minQuantity)
                .containsExactly(1, null);
        assertThat(response.slots().get(1).minSelections()).isEqualTo(1);
        assertThat(response.slots().get(1).maxSelections()).isEqualTo(2);
        assertThat(response.slots().get(1).options())
                .extracting(ConfiguredMenuTemplateResponse.ConfiguredMenuOptionResponse::minQuantity)
                .containsExactly(4, null);
    }

    @Test
    void createTemplateRejectsSelectionSlotWithoutBounds() {
        MenuItem parentMenuItem = configurableMenu(10L, 101L, 250.0);

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parentMenuItem);
        when(tenantAccessService.resolveManageableRestaurantId(101L)).thenReturn(101L);
        when(configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(false);

        assertThatThrownBy(() -> configuredMenuService.createTemplate(new ConfiguredMenuTemplateRequest(
                10L,
                true,
                List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                        "breads",
                        "Breads",
                        null,
                        2,
                        0,
                        true,
                        List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(20L, 0.0, 0, false, null))
                ))
        )))
                .hasMessageContaining("minimum and maximum selections");
    }

    @Test
    void createTemplateRejectsNegativeMinimumQuantity() {
        MenuItem parentMenuItem = configurableMenu(10L, 101L, 250.0);

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parentMenuItem);
        when(tenantAccessService.resolveManageableRestaurantId(101L)).thenReturn(101L);
        when(configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(false);

        assertThatThrownBy(() -> configuredMenuService.createTemplate(new ConfiguredMenuTemplateRequest(
                10L,
                true,
                List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                        "rice",
                        "Rice",
                        1,
                        1,
                        0,
                        true,
                        List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(20L, 0.0, 0, true, -1))
                ))
        )))
                .hasMessageContaining("Minimum quantity must be 0 or greater");
    }

    @Test
    void createTemplateRejectsSelectionSlotWithInvalidBounds() {
        MenuItem parentMenuItem = configurableMenu(10L, 101L, 250.0);

        when(inventoryService.getAccessibleMenuItem(10L)).thenReturn(parentMenuItem);
        when(tenantAccessService.resolveManageableRestaurantId(101L)).thenReturn(101L);
        when(configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(10L)).thenReturn(false);

        assertThatThrownBy(() -> configuredMenuService.createTemplate(new ConfiguredMenuTemplateRequest(
                10L,
                true,
                List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest(
                        "breads",
                        "Breads",
                        2,
                        1,
                        0,
                        true,
                        List.of(new ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest(20L, 0.0, 0, true, 4))
                ))
        )))
                .hasMessageContaining("Maximum selections must be greater than or equal to minimum selections");
    }

    private void assertCapturedTypes(List<MenuType> expectedTypes) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MenuType>> typesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(menuItemRepository).searchRecipeMenuItems(
                anyBoolean(),
                anyCollection(),
                typesCaptor.capture(),
                anyString(),
                any(PageRequest.class)
        );
        assertThat(typesCaptor.getValue()).containsExactlyInAnyOrderElementsOf(expectedTypes);
    }

    private RecipeMenuItemSearchProjection projection(Long id, Long recipeId, String itemName, String productImage) {
        RecipeMenuItemSearchProjection projection = org.mockito.Mockito.mock(RecipeMenuItemSearchProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getRecipeId()).thenReturn(recipeId);
        when(projection.getItemName()).thenReturn(itemName);
        when(projection.getProductImage()).thenReturn(productImage);
        return projection;
    }

    private MenuItem configurableMenu(Long id, Long restaurantId, Double price) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setItemName("Thali");
        menuItem.setMenuType(MenuType.CONFIGURABLE);
        ItemPrice itemPrice = new ItemPrice();
        itemPrice.setPrice(price);
        menuItem.setItemPrice(itemPrice);
        return menuItem;
    }

    private MenuItem regularMenu(Long id, Long restaurantId, String itemName) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setRestaurantId(restaurantId);
        menuItem.setItemName(itemName);
        menuItem.setMenuType(MenuType.DIRECT);
        menuItem.setItemPrice(new ItemPrice());
        return menuItem;
    }
}
