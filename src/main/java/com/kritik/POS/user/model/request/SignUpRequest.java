package com.kritik.POS.user.model.request;

import com.kritik.POS.user.model.enums.UserRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SignUpRequest {

    @NotEmpty(message = "Email is required")
    private final String email;

    @NotEmpty(message = "Password is required")
    private final String password;

    @NotEmpty(message = "Phone number is required")
    @Pattern(regexp = "^[0-9]{10}$",message = "This is not a valid phone number, Phone number must contain only digits and must be of 10 digits long")
    private final String phoneNumber;

    private final UserRole userRole;

}
