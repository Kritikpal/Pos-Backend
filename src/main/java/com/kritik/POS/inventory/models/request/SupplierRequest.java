package com.kritik.POS.inventory.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SupplierRequest(
        Long supplierId,
        Long restaurantId,
        @NotBlank(message = "Supplier name is required")
        @Size(max = 120, message = "Supplier name must be 120 characters or less")
        String supplierName,
        @Size(max = 120, message = "Contact person must be 120 characters or less")
        String contactPerson,
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{7,20}$",
                message = "Phone number must be between 7 and 20 characters and contain only digits or +()- spaces"
        )
        String phoneNumber,
        @Email(message = "Email must be valid")
        @Size(max = 120, message = "Email must be 120 characters or less")
        String email,
        @Size(max = 1000, message = "Address must be 1000 characters or less")
        String address,
        @Size(max = 50, message = "Tax identifier must be 50 characters or less")
        String taxIdentifier,
        @Size(max = 1000, message = "Notes must be 1000 characters or less")
        String notes,
        Boolean isActive
) {
}
