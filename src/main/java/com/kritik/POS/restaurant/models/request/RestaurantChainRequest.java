package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RestaurantChainRequest {

    @NotBlank(message = "Chain name is required")
    private String name;

    private String description;

    private String logoUrl;

    @Email(message = "Invalid chain email")
    private String email;

    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
    private String phoneNumber;

    private String gstNumber;

    private Boolean isActive;
}
