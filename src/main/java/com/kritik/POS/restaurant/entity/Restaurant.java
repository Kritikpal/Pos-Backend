package com.kritik.POS.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "restaurant",
    indexes = {
        @Index(name = "idx_restaurant_chain", columnList = "chain_id"),
        @Index(name = "idx_restaurant_code", columnList = "code"),
        @Index(name = "idx_restaurant_city", columnList = "city")
    }
)
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long restaurantId;

    @Column(nullable = false)
    private String name;

    // 🔑 Important for POS (billing, receipts, identification)
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    // 🔗 Tenant Mapping (MANDATORY)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chain_id")
    private RestaurantChain chain;

    @Column(name = "chain_id", insertable = false, updatable = false)
    private Long chainId;

    // 📍 Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;

    // 📞 Contact
    private String phoneNumber;
    private String email;

    // 🧾 Compliance
    private String gstNumber;

    // 🏪 POS Settings
    private String currency = "INR";
    private String timezone = "Asia/Kolkata";

    // ⚙️ Status
    private boolean isActive = true;
    private boolean isDeleted = false;

    // 🕒 Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
