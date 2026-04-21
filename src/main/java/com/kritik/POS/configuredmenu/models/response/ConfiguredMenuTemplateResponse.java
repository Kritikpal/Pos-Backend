package com.kritik.POS.configuredmenu.models.response;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuOption;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuSlot;
import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import com.kritik.POS.restaurant.util.ProductImageUrlUtil;
import com.kritik.POS.restaurant.util.RestaurantUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ConfiguredMenuTemplateResponse(
        Long id,
        Long restaurantId,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        ParentMenuItemResponse parentMenuItem,
        List<ConfiguredMenuSlotResponse> slots
) {
    public record ParentMenuItemResponse(
            Long id,
            String itemName,
            String productImage,
            BigDecimal basePrice
    ) {
    }

    public record ConfiguredMenuSlotResponse(
            Long id,
            String slotKey,
            String slotName,
            Integer minSelections,
            Integer maxSelections,
            Integer displayOrder,
            Boolean isRequired,
            List<ConfiguredMenuOptionResponse> options
    ) {
    }

    public record ConfiguredMenuOptionResponse(
            Long id,
            Long childMenuItemId,
            String childMenuItemName,
            String childMenuItemImage,
            BigDecimal priceDelta,
            Integer displayOrder,
            Boolean isDefault,
            Integer minQuantity
    ) {
        public static ConfiguredMenuOptionResponse fromEntity(ConfiguredMenuOption option) {
            return new ConfiguredMenuOptionResponse(
                    option.getId(),
                    option.getChildMenuItem().getId(),
                    option.getChildMenuItem().getItemName(),
                    option.getChildMenuItem().getProductImage() == null
                            ? null
                            : ProductImageUrlUtil.toClientUrl(option.getChildMenuItem().getProductImage().getUrl()),
                    option.getPriceDelta(),
                    option.getDisplayOrder(),
                    option.getIsDefault(),
                    option.getMinQuantity()
            );
        }
    }

    public static ConfiguredMenuTemplateResponse fromEntity(ConfiguredMenuTemplate template) {
        ParentMenuItemResponse parentMenuItem = new ParentMenuItemResponse(
                template.getParentMenuItem().getId(),
                template.getParentMenuItem().getItemName(),
                template.getParentMenuItem().getProductImage() == null
                        ? null
                        : ProductImageUrlUtil.toClientUrl(template.getParentMenuItem().getProductImage().getUrl()),
                RestaurantUtil.getMenuItemPrice(template.getParentMenuItem().getItemPrice())
        );
        List<ConfiguredMenuSlotResponse> slots = template.getSlots().stream()
                .map(slot -> new ConfiguredMenuSlotResponse(
                        slot.getId(),
                        slot.getSlotKey(),
                        slot.getSlotName(),
                        slot.getMinSelections(),
                        slot.getMaxSelections(),
                        slot.getDisplayOrder(),
                        slot.getIsRequired(),
                        slot.getOptions().stream()
                                .map(ConfiguredMenuOptionResponse::fromEntity)
                                .toList()
                ))
                .toList();
        return new ConfiguredMenuTemplateResponse(
                template.getId(),
                template.getRestaurantId(),
                template.getIsActive(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                parentMenuItem,
                slots
        );
    }
}
