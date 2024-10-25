package com.kritik.POS.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "email is required") @Email(message = "This email is not correct") String email,
        @NotNull(message = "Password is required") String password, Long restaurantId) {
}
