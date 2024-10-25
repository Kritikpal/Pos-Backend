package com.kritik.POS.exception.errors;

import org.springframework.http.HttpStatus;

public class StockException extends AppException {
    public StockException(String message) {

        super(message, HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
    }

    public StockException() {
        super("Not available in stock", HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
    }
}
