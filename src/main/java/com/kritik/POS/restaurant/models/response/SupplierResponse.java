package com.kritik.POS.restaurant.models.response;

import com.kritik.POS.inventory.entity.Supplier;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierResponse {
    private Long supplierId;
    private Long restaurantId;
    private String supplierName;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String address;
    private String taxIdentifier;
    private String notes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SupplierResponse fromEntity(Supplier supplier) {
        SupplierResponse response = new SupplierResponse();
        response.setSupplierId(supplier.getSupplierId());
        response.setRestaurantId(supplier.getRestaurantId());
        response.setSupplierName(supplier.getSupplierName());
        response.setContactPerson(supplier.getContactPerson());
        response.setPhoneNumber(supplier.getPhoneNumber());
        response.setEmail(supplier.getEmail());
        response.setAddress(supplier.getAddress());
        response.setTaxIdentifier(supplier.getTaxIdentifier());
        response.setNotes(supplier.getNotes());
        response.setIsActive(supplier.getIsActive());
        response.setCreatedAt(supplier.getCreatedAt());
        response.setUpdatedAt(supplier.getUpdatedAt());
        return response;
    }
}
