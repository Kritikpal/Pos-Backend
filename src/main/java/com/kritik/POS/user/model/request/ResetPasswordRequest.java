package com.kritik.POS.user.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    private final Long userId;

    @NotBlank(message = "New password is required")
    private final String newPassword;
}
