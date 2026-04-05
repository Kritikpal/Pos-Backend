package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.Category;
import com.kritik.POS.restaurant.projection.CategorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {

    Optional<Category> findByCategoryName(String name);
    java.util.List<Category> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("""
            select c.categoryId as categoryId,
                   c.restaurantId as restaurantId,
                   c.categoryName as categoryName,
                   c.categoryDescription as categoryDescription,
                   c.isActive as isActive,
                   c.createdAt as createdAt,
                   c.updatedAt as updatedAt
            from Category c
            where c.isDeleted = false
              and (:skipRestaurantFilter = true or c.restaurantId in :restaurantIds)
              and (:isActive is null or c.isActive = :isActive)
              and (
                  coalesce(:search, '') = ''
                  or lower(c.categoryName) like lower(concat('%', :search, '%'))
                  or lower(c.categoryDescription) like lower(concat('%', :search, '%'))
              )
            order by c.updatedAt desc, c.createdAt desc
            """)
    Page<CategorySummaryProjection> findCategorySummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                          @Param("restaurantIds") Collection<Long> restaurantIds,
                                                          @Param("isActive") Boolean isActive,
                                                          @Param("search") String search,
                                                          Pageable pageable);
}
