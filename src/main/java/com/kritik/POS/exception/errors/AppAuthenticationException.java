package com.kritik.POS.exception.errors;

import org.springframework.security.core.AuthenticationException;

public class AppAuthenticationException extends AuthenticationException {

    public AppAuthenticationException(String message) {
        super(message);
    }

    public AppAuthenticationException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
