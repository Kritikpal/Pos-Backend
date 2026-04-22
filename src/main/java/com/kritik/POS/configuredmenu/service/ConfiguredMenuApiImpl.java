package com.kritik.POS.configuredmenu.service;

import com.kritik.POS.configuredmenu.api.ConfiguredMenuApi;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuOptionSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuSlotSnapshot;
import com.kritik.POS.configuredmenu.api.ConfiguredMenuTemplateSnapshot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.configuredmenu.repository.ConfiguredMenuTemplateRepository;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.restaurant.api.MenuCatalogApi;
import com.kritik.POS.security.service.TenantAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfiguredMenuApiImpl implements ConfiguredMenuApi {

    private final ConfiguredMenuTemplateRepository configuredMenuTemplateRepository;
    private final TenantAccessService tenantAccessService;
    private final MenuCatalogApi menuCatalogApi;

    @Override
    public ConfiguredMenuTemplateSnapshot getAccessibleActiveTemplate(Long templateId) {
        ConfiguredMenuTemplate template = configuredMenuTemplateRepository.findById(templateId)
                .orElseThrow(() -> new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST));
        return toAccessibleSnapshot(template);
    }

    @Override
    public ConfiguredMenuTemplateSnapshot getAccessibleActiveTemplateByParentMenuItemId(Long parentMenuItemId) {
        ConfiguredMenuTemplate template = configuredMenuTemplateRepository.findByParentMenuItem_IdAndIsDeletedFalse(parentMenuItemId)
                .orElseThrow(() -> new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST));
        return toAccessibleSnapshot(template);
    }

    private ConfiguredMenuTemplateSnapshot toAccessibleSnapshot(ConfiguredMenuTemplate template) {
        tenantAccessService.resolveAccessibleRestaurantId(template.getRestaurantId());
        if (Boolean.TRUE.equals(template.getIsDeleted())) {
            throw new AppException("Configured menu template not found", HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new AppException("Configured menu template is inactive", HttpStatus.BAD_REQUEST);
        }

        return new ConfiguredMenuTemplateSnapshot(
                template.getId(),
                template.getRestaurantId(),
                true,
                menuCatalogApi.getAccessibleMenuItem(template.getParentMenuItem().getId()),
                template.getSlots().stream().map(this::toSlotSnapshot).toList()
        );
    }

    private ConfiguredMenuSlotSnapshot toSlotSnapshot(ConfiguredMenuSlot slot) {
        return new ConfiguredMenuSlotSnapshot(
                slot.getId(),
                slot.getSlotKey(),
                slot.getSlotName(),
                slot.getMinSelections(),
                slot.getMaxSelections(),
                Boolean.TRUE.equals(slot.getIsRequired()),
                slot.getOptions().stream().map(this::toOptionSnapshot).toList()
        );
    }

    private ConfiguredMenuOptionSnapshot toOptionSnapshot(ConfiguredMenuOption option) {
        return new ConfiguredMenuOptionSnapshot(
                option.getId(),
                option.getChildMenuItem().getId(),
                option.getChildMenuItem().getItemName(),
                option.getPriceDelta(),
                option.getMinQuantity()
        );
    }
}
