package com.kritik.POS.mobile.dto.request;

import lombok.Data;

@Data
public class SyncCursorBundle {
    private Long catalog = 0L;
    private Long price = 0L;
    private Long tax = 0L;
    private Long itemStock = 0L;
    private Long ingredientStock = 0L;
    private Long recipe = 0L;
    private Long preparedStock = 0L;
    private Long settings = 0L;
}