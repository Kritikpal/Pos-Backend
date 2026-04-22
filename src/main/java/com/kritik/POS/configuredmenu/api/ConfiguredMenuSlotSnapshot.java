package com.kritik.POS.configuredmenu.api;

import java.util.List;

public record ConfiguredMenuSlotSnapshot(
        Long id,
        String slotKey,
        String slotName,
        Integer minSelections,
        Integer maxSelections,
        boolean required,
        List<ConfiguredMenuOptionSnapshot> options
) {
}
