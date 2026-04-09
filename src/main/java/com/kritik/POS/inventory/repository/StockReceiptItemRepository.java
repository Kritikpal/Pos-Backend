package com.kritik.POS.inventory.repository;

import com.kritik.POS.inventory.entity.stockEntry.StockReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReceiptItemRepository extends JpaRepository<StockReceiptItem, Long> {
}
