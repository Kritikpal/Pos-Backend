package com.kritik.POS.admin.models.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MostOrderedMenu {
    private String menuItemName;
    private Long timesOrdered;
    private Double totalSale;
}
