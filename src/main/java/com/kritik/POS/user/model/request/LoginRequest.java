package com.kritik.POS.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Email is required") @Email(message = "This email is not correct") String email,
        @NotBlank(message = "Password is required") String password,
        Long restaurantId,
        String deviceId,
        String deviceModel,
        String manufacturer,
        String androidVersion,
        String appVersion,
        Double latitude ,
        Double longitude ,
        String locationLabel
) {
}
