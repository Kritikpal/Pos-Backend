package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.master.Ingredient;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    @EntityGraph(attributePaths = {"supplier", "baseUnit"})
    Optional<Ingredient> findBySkuAndIsDeletedFalse(String sku);
}
