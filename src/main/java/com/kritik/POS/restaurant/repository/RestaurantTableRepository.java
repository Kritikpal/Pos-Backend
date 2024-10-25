package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.DAO.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable,Long> {
}
