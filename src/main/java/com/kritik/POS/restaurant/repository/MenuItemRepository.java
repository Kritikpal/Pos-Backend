package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.DAO.MenuItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem,Long> {
   List<MenuItem> findAllByIsActiveOrderByIsTrendingDesc(boolean isActive);
}
