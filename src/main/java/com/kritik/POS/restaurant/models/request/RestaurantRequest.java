package com.kritik.POS.restaurant.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RestaurantRequest {
    @NotBlank(message = "Restaurant name is required")
    private String restaurantName;

    @Size(min = 3, max = 20, message = "Code must be 3–20 characters")
    private String code;

    private String addressLine1;
    private String addressLine2;

    @NotBlank
    private String city;

    private String state;
    private String country;
    private String pincode;

    @Pattern(regexp = "^[0-9]{10}$", message = "Invalid phone number")
    private String restaurantPhone;

    @Email
    private String restaurantEmail;

    private String restaurantGstNumber;


}
