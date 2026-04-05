package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.Supplier;
import com.kritik.POS.inventory.projection.SupplierSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierIdAndIsDeletedFalse(Long supplierId);
    List<Supplier> findAllByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("""
            select s
            from Supplier s
            where s.isDeleted = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
              and (:isActive is null or s.isActive = :isActive)
            order by s.supplierName asc
            """)
    List<Supplier> findVisibleSuppliers(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                        @Param("restaurantIds") Collection<Long> restaurantIds,
                                        @Param("isActive") Boolean isActive);

    @Query("""
            select s.supplierId as supplierId,
                   s.restaurantId as restaurantId,
                   s.supplierName as supplierName,
                   s.contactPerson as contactPerson,
                   s.phoneNumber as phoneNumber,
                   s.email as email,
                   s.isActive as isActive,
                   s.createdAt as createdAt,
                   s.updatedAt as updatedAt
            from Supplier s
            where s.isDeleted = false
              and (:skipRestaurantFilter = true or s.restaurantId in :restaurantIds)
              and (:isActive is null or s.isActive = :isActive)
              and (
                  coalesce(:search, '') = ''
                  or lower(s.supplierName) like lower(concat('%', :search, '%'))
                  or lower(coalesce(s.contactPerson, '')) like lower(concat('%', :search, '%'))
                  or lower(coalesce(s.email, '')) like lower(concat('%', :search, '%'))
                  or lower(coalesce(s.phoneNumber, '')) like lower(concat('%', :search, '%'))
              )
            order by s.updatedAt desc, s.createdAt desc
            """)
    Page<SupplierSummaryProjection> findSupplierSummaries(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                          @Param("restaurantIds") Collection<Long> restaurantIds,
                                                          @Param("isActive") Boolean isActive,
                                                          @Param("search") String search,
                                                          Pageable pageable);
}
