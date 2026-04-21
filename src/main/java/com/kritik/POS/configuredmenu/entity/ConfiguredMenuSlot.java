package com.kritik.POS.configuredmenu.entity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "configured_menu_slot")
public class ConfiguredMenuSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "template_id", nullable = false)
    private ConfiguredMenuTemplate template;

    @Column(name = "slot_key", nullable = false, length = 100)
    private String slotKey;

    @Column(name = "slot_name", nullable = false)
    private String slotName;

    @Column(name = "min_selections")
    private Integer minSelections;

    @Column(name = "max_selections")
    private Integer maxSelections;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    @OneToMany(mappedBy = "slot", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("displayOrder asc, id asc")
    private List<ConfiguredMenuOption> options = new ArrayList<>();

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
