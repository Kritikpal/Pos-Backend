package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.StockReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReceiptItemRepository extends JpaRepository<StockReceiptItem, Long> {
}
