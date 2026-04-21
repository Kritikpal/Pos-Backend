package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MenuRecipeRepository extends JpaRepository<MenuRecipe, Long> {

    @EntityGraph(attributePaths = {
            "menuItem",
            "ingredientUsages",
            "ingredientUsages.ingredientStock",
            "ingredientUsages.ingredientStock.supplier"
    })
    @Query("select m from MenuRecipe m where m.id = :id")
    Optional<MenuRecipe> findDetailedById(Long id);

    @EntityGraph(attributePaths = {
            "menuItem",
            "ingredientUsages",
            "ingredientUsages.ingredientStock",
            "ingredientUsages.ingredientStock.supplier"
    })
    @Query("select m from MenuRecipe m where m.menuItem.id = :menuItemId")
    Optional<MenuRecipe> findByMenuItemId(Long menuItemId);
}
