package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.ItemStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface StockRepository extends JpaRepository<ItemStock, String> {

    @Query("""
            select s
            from ItemStock s
            where s.isDeleted = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
            """)
    Page<ItemStock> findVisibleStocks(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                      @Param("restaurantIds") Collection<Long> restaurantIds,
                                      Pageable pageable);
}
