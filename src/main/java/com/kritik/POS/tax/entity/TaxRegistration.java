package com.kritik.POS.tax.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Data
@Table(name = "tax_registration", indexes = {
        @Index(name = "idx_tax_registration_restaurant", columnList = "restaurant_id"),
        @Index(name = "idx_tax_registration_number", columnList = "registration_number")
})
public class TaxRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "scheme_code", nullable = false, length = 40)
    private String schemeCode;

    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @Column(name = "legal_name", nullable = false, length = 180)
    private String legalName;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "region_code", length = 20)
    private String regionCode;

    @Column(name = "place_of_business", length = 255)
    private String placeOfBusiness;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
