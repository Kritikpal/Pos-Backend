package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.response.RestaurantChainInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;


public interface RestaurantChainRepository extends JpaRepository<RestaurantChain, Long> {
    boolean existsByName(String name);

    Optional<RestaurantChain> findByName(String name);

    @Query("select r from RestaurantChain r where r.chainId = :chainId or upper(r.name) = upper(:name)")
    Page<RestaurantChainInfo> findByChainIdOrNameIgnoreCase(@Param("chainId") @Nullable Long chainId,
                                                            @Param("name") @Nullable String name,
                                                            Pageable pageable);
}