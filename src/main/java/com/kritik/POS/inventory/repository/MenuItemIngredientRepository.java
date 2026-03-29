package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.MenuItemIngredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemIngredientRepository extends JpaRepository<MenuItemIngredient, Long> {

    @EntityGraph(attributePaths = {"menuItem", "menuItem.itemStock", "ingredientStock"})
    List<MenuItemIngredient> findAllByIngredientStockSku(String ingredientSku);
}
