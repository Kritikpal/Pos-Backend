package com.kritik.POS.inventory.entity.recipi;

import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "menu_recipe",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recipe_menu", columnNames = {"menu_item_id"})
        })
public class MenuRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @OneToOne
    @JoinColumn(name = "menu_item_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuItem menuItem;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MenuItemIngredient> ingredientUsages = new ArrayList<>();

    @Version
    @Column(name = "recipe_version")
    private Integer recipeVersion = 0;

    @CreatedDate
    @Column(name = "created_date")
    private Instant createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Instant lastModifiedDate;

}
