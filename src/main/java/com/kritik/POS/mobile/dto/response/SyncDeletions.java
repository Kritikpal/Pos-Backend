package com.kritik.POS.mobile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDeletions {

    @Builder.Default
    private List<Long> categories = new ArrayList<>();

    @Builder.Default
    private List<Long> menuItems = new ArrayList<>();

    @Builder.Default
    private List<Long> taxes = new ArrayList<>();

    @Builder.Default
    private List<Long> taxClasses = new ArrayList<>();

    @Builder.Default
    private List<Long> taxDefinitions = new ArrayList<>();

    @Builder.Default
    private List<Long> taxRules = new ArrayList<>();

    @Builder.Default
    private List<Long> taxRegistrations = new ArrayList<>();

    @Builder.Default
    private List<String> itemStocks = new ArrayList<>();

    @Builder.Default
    private List<String> ingredientStocks = new ArrayList<>();
}
