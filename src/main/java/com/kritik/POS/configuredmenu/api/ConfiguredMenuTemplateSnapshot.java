package com.kritik.POS.configuredmenu.api;

import com.kritik.POS.restaurant.api.MenuItemSnapshot;
import java.util.List;

public record ConfiguredMenuTemplateSnapshot(
        Long id,
        Long restaurantId,
        boolean active,
        MenuItemSnapshot parentMenuItem,
        List<ConfiguredMenuSlotSnapshot> slots
) {
}
