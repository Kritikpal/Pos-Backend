package com.kritik.POS.tax.repository;

import com.kritik.POS.tax.entity.TaxRegistration;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxRegistrationRepository extends JpaRepository<TaxRegistration, Long> {

    @Query("""
            select t
            from TaxRegistration t
            where (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
              and (:isActive is null or t.isActive = :isActive)
            order by t.updatedAt desc, t.createdAt desc
            """)
    Page<TaxRegistration> findVisible(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                      @Param("restaurantIds") Collection<Long> restaurantIds,
                                      @Param("isActive") Boolean isActive,
                                      Pageable pageable);

    Optional<TaxRegistration> findFirstByRestaurantIdAndIsDefaultTrueAndIsActiveTrueOrderByIdAsc(Long restaurantId);
}
