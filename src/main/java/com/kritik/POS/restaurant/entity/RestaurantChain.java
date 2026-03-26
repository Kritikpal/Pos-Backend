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
    name = "restaurant_chain",
    indexes = {
        @Index(name = "idx_chain_name", columnList = "name")
    }
)
public class RestaurantChain {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long chainId;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // 🌍 Branding
    private String logoUrl;

    // 📞 Contact
    private String email;
    private String phoneNumber;

    // 🧾 Business Info
    private String gstNumber;

    // ⚙️ Status
    private boolean isActive = true;
    private boolean isDeleted = false;

    // 🕒 Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}