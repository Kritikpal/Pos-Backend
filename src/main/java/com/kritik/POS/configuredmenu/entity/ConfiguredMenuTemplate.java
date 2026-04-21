package com.kritik.POS.configuredmenu.entity;

import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "configured_menu_template")
public class ConfiguredMenuTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    @jakarta.persistence.JoinColumn(name = "parent_menu_item_id", nullable = false)
    private MenuItem parentMenuItem;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("displayOrder asc, id asc")
    private List<ConfiguredMenuSlot> slots = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Long version = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @jakarta.persistence.PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
