package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.production.ProductionEntry;
import com.kritik.POS.inventory.projection.ProductionEntrySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface ProductionEntryRepository extends JpaRepository<ProductionEntry, Long> {

    Optional<ProductionEntry> findByIdAndRestaurantIdIn(Long id, Collection<Long> restaurantIds);

    @Query("""
            select pe.id as id,
                   pe.restaurantId as restaurantId,
                   pe.menuItemId as menuItemId,
                   m.itemName as menuItemName,
                   pe.producedQty as producedQty,
                   pe.unitCode as unitCode,
                   pe.productionTime as productionTime,
                   pe.createdBy as createdBy,
                   pe.createdAt as createdAt
            from ProductionEntry pe
            left join MenuItem m on m.id = pe.menuItemId
            where (:skipRestaurantFilter = true or pe.restaurantId in :restaurantIds)
              and (:menuItemId is null or pe.menuItemId = :menuItemId)
            order by pe.productionTime desc, pe.createdAt desc
            """)
    Page<ProductionEntrySummaryProjection> findSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                         @Param("restaurantIds") Collection<Long> restaurantIds,
                                                         @Param("menuItemId") Long menuItemId,
                                                         Pageable pageable);
}
