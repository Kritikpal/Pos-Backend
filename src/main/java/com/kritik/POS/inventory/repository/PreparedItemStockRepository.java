package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.stock.PreparedItemStock;
import com.kritik.POS.inventory.projection.PreparedStockDeductionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface PreparedItemStockRepository extends JpaRepository<PreparedItemStock, Long> {

    @Query("""
        select ps.menuItemId as menuItemId,
               sum(si.amount) as quantity
        from SaleItem si
        join si.order o
        join si.menuItem m
        join PreparedItemStock ps on ps.menuItemId = m.id
        where o.orderId = :orderId
          and o.isDeleted = false
          and si.isDeleted = false
          and coalesce(m.isPrepared, false) = true
        group by ps.menuItemId
        """)
    List<PreparedStockDeductionProjection> findPreparedStockDeductionsByOrderId(@Param("orderId") String orderId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update PreparedItemStock ps
        set ps.availableQty = ps.availableQty - :quantity,
            ps.updatedAt = :updatedAt
        where ps.menuItemId = :menuItemId
          and ps.active = true
          and (ps.availableQty - ps.reservedQty) >= :quantity
        """)
    int deductPreparedStockIfAvailable(@Param("menuItemId") Long menuItemId,
                                       @Param("quantity") Double quantity,
                                       @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update PreparedItemStock ps
        set ps.availableQty = ps.availableQty + :quantity,
            ps.updatedAt = :updatedAt
        where ps.menuItemId = :menuItemId
          and ps.active = true
        """)
    int increasePreparedStock(@Param("menuItemId") Long menuItemId,
                              @Param("quantity") Double quantity,
                              @Param("updatedAt") LocalDateTime updatedAt);


}
