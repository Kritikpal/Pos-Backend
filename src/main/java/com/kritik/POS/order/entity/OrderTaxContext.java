package com.kritik.POS.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.util.Objects;

@Entity
@Data
@Table(name = "order_tax_context")
public class OrderTaxContext {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    @ToString.Exclude
    private Order order;

    @Column(name = "seller_tax_registration_id")
    private Long sellerTaxRegistrationId;

    @Column(name = "seller_registration_number_snapshot")
    private String sellerRegistrationNumberSnapshot;

    @Column(name = "seller_country_code")
    private String sellerCountryCode;

    @Column(name = "seller_region_code")
    private String sellerRegionCode;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "buyer_tax_id")
    private String buyerTaxId;

    @Column(name = "buyer_tax_category")
    private String buyerTaxCategory;

    @Column(name = "buyer_country_code")
    private String buyerCountryCode;

    @Column(name = "buyer_region_code")
    private String buyerRegionCode;

    @Column(name = "billing_address_json", columnDefinition = "TEXT")
    private String billingAddressJson;

    @Column(name = "place_of_supply_country_code")
    private String placeOfSupplyCountryCode;

    @Column(name = "place_of_supply_region_code")
    private String placeOfSupplyRegionCode;

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

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        OrderTaxContext that = (OrderTaxContext) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
