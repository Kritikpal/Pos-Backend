package com.kritik.POS.configuredmenu.service.Impl;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.models.request.ConfiguredMenuTemplateRequest;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuItemSearchDto;
import com.kritik.POS.configuredmenu.models.response.ConfiguredMenuTemplateResponse;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.configuredmenu.service.ConfiguredMenuService;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.inventory.service.InventoryService;
import com.kritik.POS.restaurant.entity.MenuItem;
import com.kritik.POS.restaurant.entity.enums.MenuType;
import com.kritik.POS.restaurant.repository.MenuItemRepository;
import com.kritik.POS.security.service.TenantAccessService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConfiguredMenuServiceImpl implements ConfiguredMenuService {

    private final ConfiguredMenuTemplateRepository configuredMenuTemplateRepository;
    private final MenuItemRepository menuItemRepository;
    private final InventoryService inventoryService;
    private final TenantAccessService tenantAccessService;

    @Override
    @Transactional
    public ConfiguredMenuTemplateResponse createTemplate(ConfiguredMenuTemplateRequest request) {
        MenuItem parentMenuItem = loadManageableMenuItem(request.parentMenuItemId(), true);
        if (configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(parentMenuItem.getId())) {
            throw new AppException("Configured menu template already exists for selected parent menu item", HttpStatus.BAD_REQUEST);
        }

        ConfiguredMenuTemplate template = new ConfiguredMenuTemplate();
        applyTemplateRequest(template, request, parentMenuItem);
        ConfiguredMenuTemplate savedTemplate = configuredMenuTemplateRepository.save(template);
        return ConfiguredMenuTemplateResponse.fromEntity(savedTemplate);
    }

    @Override
    @Transactional
    public ConfiguredMenuTemplateResponse updateTemplate(Long id, ConfiguredMenuTemplateRequest request) {
        ConfiguredMenuTemplate template = getManageableTemplate(id);
        MenuItem parentMenuItem = loadManageableMenuItem(request.parentMenuItemId(), true);
        if (!template.getParentMenuItem().getId().equals(parentMenuItem.getId())
                && configuredMenuTemplateRepository.existsByParentMenuItem_IdAndIsDeletedFalse(parentMenuItem.getId())) {
            throw new AppException("Configured menu template already exists for selected parent menu item", HttpStatus.BAD_REQUEST);
        }

        applyTemplateRequest(template, request, parentMenuItem);
        ConfiguredMenuTemplate savedTemplate = configuredMenuTemplateRepository.save(template);
        return ConfiguredMenuTemplateResponse.fromEntity(savedTemplate);
    }

    @Override
    @Transactional
    public ConfiguredMenuTemplateResponse getTemplate(Long id) {
        return ConfiguredMenuTemplateResponse.fromEntity(getAccessibleTemplate(id));
    }

    @Override
    @Transactional
    public List<ConfiguredMenuTemplateResponse> getTemplates(Long chainId, Long restaurantId, Boolean isActive, String search) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }

        return configuredMenuTemplateRepository.findVisibleTemplates(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        isActive,
                        normalizeSearch(search)
                ).stream()
                .map(ConfiguredMenuTemplateResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public ConfiguredMenuTemplateResponse previewTemplate(Long id) {
        return ConfiguredMenuTemplateResponse.fromEntity(getAccessibleTemplate(id));
    }

    @Override
    @Transactional
    public boolean deleteTemplate(Long id) {
        ConfiguredMenuTemplate template = getManageableTemplate(id);
        template.setIsDeleted(true);
        template.setIsActive(false);
        template.getParentMenuItem().setIsAvailable(false);
        configuredMenuTemplateRepository.save(template);
        return true;
    }

    @Override
    public List<ConfiguredMenuItemSearchDto> searchCandidateMenuItems(Long chainId,
                                                                      Long restaurantId,
                                                                      String search,
                                                                      Boolean recipeBased,
                                                                      Integer limit) {
        List<Long> accessibleRestaurantIds = tenantAccessService.resolveAccessibleRestaurantIds(chainId, restaurantId);
        if (!tenantAccessService.isSuperAdmin() && accessibleRestaurantIds.isEmpty()) {
            return List.of();
        }

        int pageSize = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        return menuItemRepository.searchRecipeMenuItems(
                        tenantAccessService.isSuperAdmin(),
                        tenantAccessService.queryRestaurantIds(accessibleRestaurantIds),
                        Set.of(MenuType.RECIPE,MenuType.PREPARED,MenuType.DIRECT),
                        normalizeSearch(search),
                        org.springframework.data.domain.PageRequest.of(0, pageSize)
                ).stream()
                .map(ConfiguredMenuItemSearchDto::fromProjection)
                .toList();
    }

    private void applyTemplateRequest(ConfiguredMenuTemplate template,
                                      ConfiguredMenuTemplateRequest request,
                                      MenuItem parentMenuItem) {
        Long manageableRestaurantId = tenantAccessService.resolveManageableRestaurantId(parentMenuItem.getRestaurantId());
        validateSlots(request.slots());

        template.setParentMenuItem(parentMenuItem);
        template.setRestaurantId(manageableRestaurantId);
        template.setIsDeleted(false);
        template.setIsActive(request.isActive() == null ? Boolean.TRUE : request.isActive());
        parentMenuItem.setIsAvailable(template.getIsActive());

        List<ConfiguredMenuSlot> nextSlots = new ArrayList<>();
        Set<String> slotKeys = new HashSet<>();
        for (ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest slotRequest : request.slots()) {
            String normalizedSlotKey = slotRequest.slotKey().trim().toLowerCase();
            if (!slotKeys.add(normalizedSlotKey)) {
                throw new AppException("Duplicate slot key: " + slotRequest.slotKey(), HttpStatus.BAD_REQUEST);
            }

            ConfiguredMenuSlot slot = new ConfiguredMenuSlot();
            slot.setTemplate(template);
            slot.setSlotKey(normalizedSlotKey);
            slot.setSlotName(slotRequest.slotName().trim());
            slot.setMinSelections(slotRequest.minSelections());
            slot.setMaxSelections(slotRequest.maxSelections());
            slot.setDisplayOrder(slotRequest.displayOrder());
            slot.setIsRequired(resolveSlotRequired(slotRequest));

            List<ConfiguredMenuOption> nextOptions = new ArrayList<>();
            Set<Long> childMenuIds = new HashSet<>();
            for (ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest optionRequest : slotRequest.options()) {
                if (!childMenuIds.add(optionRequest.childMenuItemId())) {
                    throw new AppException("Duplicate option menu item in slot " + slotRequest.slotName(), HttpStatus.BAD_REQUEST);
                }
                validateOptionConstraints(optionRequest);

                MenuItem childMenuItem = loadManageableMenuItem(optionRequest.childMenuItemId(), false);
                if (!manageableRestaurantId.equals(childMenuItem.getRestaurantId())) {
                    throw new AppException("All slot options must belong to the same restaurant as the parent menu item", HttpStatus.BAD_REQUEST);
                }
                if (parentMenuItem.getId().equals(childMenuItem.getId())) {
                    throw new AppException("Parent menu item cannot be used as an option inside its own template", HttpStatus.BAD_REQUEST);
                }
                if (childMenuItem.getMenuType() == MenuType.CONFIGURABLE) {
                    throw new AppException("Configurable menu items cannot be nested as child options", HttpStatus.BAD_REQUEST);
                }

                ConfiguredMenuOption option = new ConfiguredMenuOption();
                option.setSlot(slot);
                option.setChildMenuItem(childMenuItem);
                option.setPriceDelta(optionRequest.priceDelta());
                option.setDisplayOrder(optionRequest.displayOrder());
                option.setIsDefault(optionRequest.isDefault() != null && optionRequest.isDefault());
                option.setMinQuantity(optionRequest.minQuantity());
                nextOptions.add(option);
            }

            slot.getOptions().clear();
            slot.getOptions().addAll(nextOptions);
            nextSlots.add(slot);
        }

        template.getSlots().clear();
        template.getSlots().addAll(nextSlots);
    }

    private void validateSlots(List<ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest> slotRequests) {
        for (ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest slotRequest : slotRequests) {
            validateSelectionSlot(slotRequest, resolveSlotRequired(slotRequest));
        }
    }

    private void validateSelectionSlot(ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest slotRequest, boolean required) {
        if (slotRequest.minSelections() == null || slotRequest.maxSelections() == null) {
            throw new AppException("Slots require minimum and maximum selections", HttpStatus.BAD_REQUEST);
        }
        if (slotRequest.maxSelections() < slotRequest.minSelections()) {
            throw new AppException("Maximum selections must be greater than or equal to minimum selections", HttpStatus.BAD_REQUEST);
        }
        if (required && slotRequest.minSelections() < 1) {
            throw new AppException("Required slots must allow at least one selection", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateOptionConstraints(ConfiguredMenuTemplateRequest.ConfiguredMenuOptionRequest optionRequest) {
        if (optionRequest.minQuantity() != null && optionRequest.minQuantity() < 0) {
            throw new AppException("Minimum quantity must be 0 or greater", HttpStatus.BAD_REQUEST);
        }
    }

    private MenuItem loadManageableMenuItem(Long menuItemId, boolean requireParentPrice) {
        MenuItem menuItem = inventoryService.getAccessibleMenuItem(menuItemId);
        tenantAccessService.resolveManageableRestaurantId(menuItem.getRestaurantId());
        if (Boolean.TRUE.equals(menuItem.getIsDeleted())) {
            throw new AppException("Menu item is not valid", HttpStatus.BAD_REQUEST);
        }
        if (requireParentPrice && menuItem.getMenuType() != MenuType.CONFIGURABLE) {
            throw new AppException("Parent menu item must use CONFIGURABLE menu type", HttpStatus.BAD_REQUEST);
        }
        if (!requireParentPrice && menuItem.getMenuType() == MenuType.CONFIGURABLE) {
            throw new AppException("Configured parent menu items cannot be used as child options", HttpStatus.BAD_REQUEST);
        }
        if (requireParentPrice && (menuItem.getItemPrice() == null || menuItem.getItemPrice().getPrice() == null)) {
            throw new AppException("Parent menu item must have a valid price", HttpStatus.BAD_REQUEST);
        }
        return menuItem;
    }

    private ConfiguredMenuTemplate getAccessibleTemplate(Long id) {
        ConfiguredMenuTemplate template = configuredMenuTemplateRepository.findById(id)
                .orElseThrow(() -> new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST));
        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST);
        }
        if (!tenantAccessService.isSuperAdmin()) {
            tenantAccessService.resolveAccessibleRestaurantId(template.getRestaurantId());
        }
        return template;
    }

    private ConfiguredMenuTemplate getManageableTemplate(Long id) {
        ConfiguredMenuTemplate template = getAccessibleTemplate(id);
        tenantAccessService.resolveManageableRestaurantId(template.getRestaurantId());
        return template;
    }

    private String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? "" : normalized;
    }

    private Set<MenuType> resolveCandidateSearchTypes(Boolean recipeBased) {
        if (Boolean.TRUE.equals(recipeBased)) {
            return Set.of(MenuType.RECIPE, MenuType.PREPARED);
        }
        if (Boolean.FALSE.equals(recipeBased)) {
            return Set.of(MenuType.CONFIGURABLE);
        }
        return Set.of(MenuType.CONFIGURABLE, MenuType.RECIPE, MenuType.PREPARED);
    }

    private boolean resolveSlotRequired(ConfiguredMenuTemplateRequest.ConfiguredMenuSlotRequest slotRequest) {
        if (slotRequest.isRequired() != null) {
            return slotRequest.isRequired();
        }
        return slotRequest.minSelections() != null && slotRequest.minSelections() > 0;
    }
}
