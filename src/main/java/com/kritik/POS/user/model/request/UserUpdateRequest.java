package com.kritik.POS.user.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @Email(message = "Invalid email")
    private final String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private final String phoneNumber;

    private final String role;

    private final Long chainId;
    private final Long restaurantId;
}
