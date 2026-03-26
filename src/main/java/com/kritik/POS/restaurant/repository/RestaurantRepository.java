package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.Arrays;
import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
  boolean existsByCodeAndChain(String code, RestaurantChain chain);

  @Query("SELECT r FROM Restaurant r JOIN FETCH r.chain")
  List<Restaurant> findAllWithChain();

  @Query("""
          select r.chain.name as chainName,
                 r.chain.chainId as chainId,
                 r.name as resturantName,
                 r.email as adminEmail,
                 r.code as code
                 from Restaurant r
          where r.chain.chainId = :chainId or upper(r.name) = upper(:name)
                    and r.isActive = true and
                    r.isDeleted = false or
                    r.restaurantId = :restaurantId
                    order by r.updatedAt""")
  Page<RestaurantProjection> findRestaurants(@Param("chainId") @Nullable Long chainId,
                                             @Param("name") @Nullable String name,
                                             @Param("restaurantId") @Nullable Long restaurantId,
                                             Pageable pageable);
}