package com.kritik.POS.common.repository;

import com.kritik.POS.restaurant.DAO.ProductFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<ProductFile,Long> {

}
