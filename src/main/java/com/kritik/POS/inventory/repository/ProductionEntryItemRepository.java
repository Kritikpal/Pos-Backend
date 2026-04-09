package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.production.ProductionEntryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductionEntryItemRepository extends JpaRepository<ProductionEntryItem, Long> {

    List<ProductionEntryItem> findAllByProductionEntry_IdOrderByIdAsc(Long productionEntryId);
}
