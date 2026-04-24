package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.enums.UnitConversionSourceType;
import com.kritik.POS.inventory.entity.unit.ItemUnitConversion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemUnitConversionRepository extends JpaRepository<ItemUnitConversion, Long> {

    @EntityGraph(attributePaths = {"unit"})
    Optional<ItemUnitConversion> findByRestaurantIdAndSourceTypeAndSourceIdAndUnitIdAndActiveTrue(
            Long restaurantId,
            UnitConversionSourceType sourceType,
            String sourceId,
            Long unitId
    );

    @EntityGraph(attributePaths = {"unit"})
    @Query("""
            select c
            from ItemUnitConversion c
            join c.unit u
            where c.restaurantId = :restaurantId
              and c.sourceType = :sourceType
              and c.sourceId = :sourceId
            order by lower(u.code), c.id
            """)
    List<ItemUnitConversion> findAllForSource(@Param("restaurantId") Long restaurantId,
                                              @Param("sourceType") UnitConversionSourceType sourceType,
                                              @Param("sourceId") String sourceId);

    boolean existsByRestaurantIdAndSourceTypeAndSourceIdAndUnitId(
            Long restaurantId,
            UnitConversionSourceType sourceType,
            String sourceId,
            Long unitId
    );

    @EntityGraph(attributePaths = {"unit"})
    @Query("""
            select c
            from ItemUnitConversion c
            join c.unit u
            where c.restaurantId = :restaurantId
              and c.sourceType = :sourceType
              and c.sourceId in :sourceIds
              and c.active = true
              and c.purchaseAllowed = true
            order by lower(u.code), c.id
            """)
    List<ItemUnitConversion> findActivePurchaseConversions(@Param("restaurantId") Long restaurantId,
                                                           @Param("sourceType") UnitConversionSourceType sourceType,
                                                           @Param("sourceIds") Collection<String> sourceIds);

    List<ItemUnitConversion> findAllByRestaurantIdAndSourceTypeAndSourceId(
            Long restaurantId,
            UnitConversionSourceType sourceType,
            String sourceId
    );
}
