package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.master.Ingredient;
import com.kritik.POS.inventory.entity.stock.IngredientStock;
import com.kritik.POS.inventory.entity.unit.UnitMaster;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class IngredientPersistenceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private IngredientStockRepository ingredientStockRepository;

    @Test
    void saveAllowsCreatingIngredientAndIngredientStockWithAssignedSku() {
        UnitMaster unitMaster = new UnitMaster();
        unitMaster.setCode("KG");
        unitMaster.setDisplayName("Kilogram");
        unitMaster.setActive(true);
        entityManager.persist(unitMaster);

        Ingredient ingredient = new Ingredient();
        ingredient.setSku("ING-PERSIST-1");
        ingredient.setRestaurantId(10L);
        ingredient.setIngredientName("Paneer");
        ingredient.setBaseUnit(unitMaster);
        ingredient.setIsActive(true);
        ingredient.setIsDeleted(false);
        ingredient = ingredientRepository.saveAndFlush(ingredient);

        IngredientStock ingredientStock = new IngredientStock();
        ingredientStock.setSku(ingredient.getSku());
        ingredientStock.setIngredient(ingredient);
        ingredientStock.setRestaurantId(ingredient.getRestaurantId());
        ingredientStock.setIngredientName(ingredient.getIngredientName());
        ingredientStock.setTotalStock(0.0);
        ingredientStock.setReorderLevel(2.0);
        ingredientStock.setUnitOfMeasure("KG");
        ingredientStock.setIsActive(true);
        ingredientStock.setIsDeleted(false);
        ingredientStockRepository.saveAndFlush(ingredientStock);

        entityManager.clear();

        IngredientStock saved = ingredientStockRepository.findById("ING-PERSIST-1").orElseThrow();
        assertThat(saved.getSku()).isEqualTo("ING-PERSIST-1");
        assertThat(saved.getIngredient()).isNotNull();
        assertThat(saved.getIngredient().getIngredientName()).isEqualTo("Paneer");
    }
}
