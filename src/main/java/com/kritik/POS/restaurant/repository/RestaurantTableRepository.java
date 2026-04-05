package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.RestaurantTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long>, JpaSpecificationExecutor<RestaurantTable> {
    List<RestaurantTable> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("""
            select t
            from RestaurantTable t
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
            order by t.tableNumber
            """)
    List<RestaurantTable> findVisibleTables(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                            @Param("restaurantIds") Collection<Long> restaurantIds);

    @Query("""
            select t
            from RestaurantTable t
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
            order by t.tableNumber
            """)
    Page<RestaurantTable> findVisibleTables(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                            @Param("restaurantIds") Collection<Long> restaurantIds,
                                            Pageable pageable);
}
