package com.kritik.POS.user.entity;

public enum PasswordResetStatus {
    PENDING,    // Reset request created, email sent
    VERIFIED,   // Token verified, password changed
    EXPIRED,    // Token expired without use
    USED        // Token already used for password reset
}
