package com.kritik.POS.restaurant.models.request;

import lombok.Data;

import jakarta.validation.constraints.*;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RestaurantSetupRequest extends RestaurantRequest {

    // =========================
    // 🏢 CHAIN
    // =========================
    @NotBlank(message = "Chain name is required")
    private String chainName;

    private String description;

    private String logoUrl;

    @Email(message = "Invalid cain admin email")
    private String chainEmail;

    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
    private String chainPhone;

    private String chainGstNumber;

    // =========================
    // 👤 ADMIN USER
    // =========================
    @Email(message = "Invalid admin email")
    @NotBlank
    private String adminEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String adminPassword;

    @Pattern(regexp = "^[0-9]{10}$")
    private String adminPhone;





}