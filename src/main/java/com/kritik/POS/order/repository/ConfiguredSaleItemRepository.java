package com.kritik.POS.order.repository;

import com.kritik.POS.order.entity.ConfiguredSaleItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfiguredSaleItemRepository extends JpaRepository<ConfiguredSaleItem, Long> {

    @EntityGraph(attributePaths = {"selections"})
    List<ConfiguredSaleItem> findAllByOrder_IdOrderByIdAsc(Long orderId);

    boolean existsByOrder_Id(Long orderId);
}
