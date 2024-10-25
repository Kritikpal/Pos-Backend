package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.DAO.ItemStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<ItemStock,String> {



}
