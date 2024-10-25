package com.kritik.POS.exception.errors;

import org.springframework.http.HttpStatus;

public class OrderException extends AppException {

    public OrderException() {
        super("Order not found", HttpStatus.BAD_REQUEST);
    }

    public OrderException(String message) {
        super(message, HttpStatus.PAYMENT_REQUIRED);
    }

    public OrderException(String message, HttpStatus httpStatus) {
        super(message, httpStatus);
    }
}
