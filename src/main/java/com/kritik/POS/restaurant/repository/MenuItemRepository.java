package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem,Long> {
   List<MenuItem> findAllByIsActiveOrderByIsTrendingDesc(boolean isActive);

   @Query("""
    SELECT m FROM MenuItem m
    WHERE m.isActive = true
    AND (:search IS NULL OR LOWER(m.itemName) LIKE LOWER(CONCAT('%', :search, '%')))
    AND (:categoryId IS NULL OR m.category.categoryId = :categoryId)
    ORDER BY m.isTrending DESC, m.createdAt DESC
""")
   Page<MenuItem> searchDashboardItems(
           @Param("search") String search,
           @Param("categoryId") Long categoryId,
           Pageable pageable
   );}
