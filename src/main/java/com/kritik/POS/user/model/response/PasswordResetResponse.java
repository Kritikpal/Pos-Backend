package com.kritik.POS.user.model.response;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class PasswordResetResponse {
    private String message;
    private String status;
}
