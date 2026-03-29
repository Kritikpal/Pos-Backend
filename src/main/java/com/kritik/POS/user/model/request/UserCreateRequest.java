package com.kritik.POS.user.model.request;

import com.kritik.POS.user.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserCreateRequest {
    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private final String email;

    @NotBlank(message = "Password is required")
    private final String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private final String phoneNumber;

    @NotBlank(message = "Role is required")
    private final String role;

    private final Long chainId;
    private final Long restaurantId;
}
