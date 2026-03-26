package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.ItemPrice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPriceRepository extends JpaRepository<ItemPrice,Long> {
}
