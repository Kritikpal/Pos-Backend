package com.kritik.POS.tax.repository;

import com.kritik.POS.tax.entity.TaxDefinition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxDefinitionRepository extends JpaRepository<TaxDefinition, Long> {

    Optional<TaxDefinition> findByRestaurantIdAndCodeAndIsDeletedFalse(Long restaurantId, String code);

    @Query("""
            select t
            from TaxDefinition t
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
              and (:isActive is null or t.isActive = :isActive)
              and (
                    coalesce(:search, '') = ''
                    or lower(t.code) like lower(concat('%', :search, '%'))
                    or lower(t.displayName) like lower(concat('%', :search, '%'))
              )
            order by t.updatedAt desc, t.createdAt desc
            """)
    Page<TaxDefinition> findVisible(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                    @Param("restaurantIds") Collection<Long> restaurantIds,
                                    @Param("isActive") Boolean isActive,
                                    @Param("search") String search,
                                    Pageable pageable);

    List<TaxDefinition> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    List<TaxDefinition> findAllByIdIn(Collection<Long> ids);
}
