package com.kritik.POS.tax.repository;

import com.kritik.POS.tax.entity.TaxRate;
import com.kritik.POS.tax.projection.ActiveTaxRateProjection;
import com.kritik.POS.tax.projection.TaxRateSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    @Query("""
            select t
            from TaxRate t
            where t.isActive = true
              and t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
            order by t.taxName
            """)
    List<TaxRate> findAllActiveVisible(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                       @Param("restaurantIds") Collection<Long> restaurantIds);

    @Query("""
            select t.taxName as name,
                   t.taxAmount as taxRate
            from TaxRate t
            where t.isActive = true
              and t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
            order by t.taxName
            """)
    List<ActiveTaxRateProjection> findActiveTaxRateSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                             @Param("restaurantIds") Collection<Long> restaurantIds);

    @Query("""
            select t.taxId as taxId,
                   t.restaurantId as restaurantId,
                   t.taxName as taxName,
                   t.taxAmount as taxAmount,
                   t.isActive as isActive,
                   t.createdAt as createdAt,
                   t.updatedAt as updatedAt
            from TaxRate t
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
              and (:isActive is null or t.isActive = :isActive)
              and (
                  coalesce(:search, '') = ''
                  or lower(t.taxName) like lower(concat('%', :search, '%'))
              )
            order by t.updatedAt desc, t.createdAt desc
            """)
    Page<TaxRateSummaryProjection> findTaxSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                    @Param("restaurantIds") Collection<Long> restaurantIds,
                                                    @Param("isActive") Boolean isActive,
                                                    @Param("search") String search,
                                                    Pageable pageable);
}
