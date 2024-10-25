package com.kritik.POS.tax;

import lombok.Getter;

@Getter
public enum TaxErrorMessage {
    INVALID_TAX_ID("Invalid tax id");
    private final String message;
    TaxErrorMessage(String message) {
        this.message = message;
    }
}
