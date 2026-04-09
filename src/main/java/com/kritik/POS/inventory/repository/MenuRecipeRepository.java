package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.recipi.MenuRecipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRecipeRepository extends JpaRepository<MenuRecipe, Long> {
}
