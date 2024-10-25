package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.DAO.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category,Long> {

    Optional<Category> findByCategoryName(String name);
}
