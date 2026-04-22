package com.kritik.POS.email.api.models;

public record PasswordResetEmailRequested(String to, String resetToken) {
}
