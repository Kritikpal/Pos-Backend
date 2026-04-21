package com.kritik.POS.tax.repository;

import com.kritik.POS.tax.entity.TaxRule;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxRuleRepository extends JpaRepository<TaxRule, Long> {

    @Query("""
            select t
            from TaxRule t
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
              and (:isActive is null or t.isActive = :isActive)
            order by t.priority desc, t.sequenceNo asc, t.updatedAt desc
            """)
    Page<TaxRule> findVisible(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                              @Param("restaurantIds") Collection<Long> restaurantIds,
                              @Param("isActive") Boolean isActive,
                              Pageable pageable);

    @Query("""
            select t
            from TaxRule t
            where t.isDeleted = false
              and t.isActive = true
              and t.restaurantId = :restaurantId
            order by t.sequenceNo asc, t.priority desc, t.id asc
            """)
    List<TaxRule> findActiveForRestaurant(@Param("restaurantId") Long restaurantId);

    List<TaxRule> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);
}
