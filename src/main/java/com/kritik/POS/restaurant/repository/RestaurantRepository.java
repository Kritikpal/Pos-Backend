package com.kritik.POS.restaurant.repository;

import com.kritik.POS.restaurant.entity.Restaurant;
import com.kritik.POS.restaurant.entity.RestaurantChain;
import com.kritik.POS.restaurant.models.response.RestaurantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {
    boolean existsByCodeAndChain(String code, RestaurantChain chain);

    boolean existsByCodeAndChainAndRestaurantIdNot(String code, RestaurantChain chain, Long restaurantId);

    boolean existsByRestaurantIdAndChainIdAndIsDeletedFalse(Long restaurantId, Long chainId);

    @Query("SELECT r FROM Restaurant r JOIN FETCH r.chain WHERE r.isDeleted = false")
    List<Restaurant> findAllWithChain();

    @Query("""
            select r.chain.name as chainName,
                   r.restaurantId as restaurantId,
                   r.chainId as chainId,
                   r.name as resturantName,
                   r.email as adminEmail,
                   r.code as code
            from Restaurant r
            where (:chainId is null or r.chainId = :chainId)
              and (:restaurantId is null or r.restaurantId = :restaurantId)
              and (:isActive is null or r.isActive = :isActive)
              and (
                  coalesce(:name, '') = ''
                  or lower(r.name) like lower(concat('%', :name, '%'))
                  or lower(r.code) like lower(concat('%', :name, '%'))
              )
              and r.isDeleted = false
            order by r.updatedAt desc, r.createdAt desc
            """)
    Page<RestaurantProjection> findRestaurants(@Param("chainId") @Nullable Long chainId,
                                               @Param("name") @Nullable String name,
                                               @Param("restaurantId") @Nullable Long restaurantId,
                                               @Param("isActive") @Nullable Boolean isActive,
                                               Pageable pageable);

    @Query("""
            select r.restaurantId
            from Restaurant r
            where r.chainId = :chainId
              and r.isActive = true
              and r.isDeleted = false
            order by r.restaurantId
            """)
    List<Long> findActiveRestaurantIdsByChainId(@Param("chainId") Long chainId);

    Optional<Restaurant> findByRestaurantIdAndIsDeletedFalse(Long restaurantId);

    @Query("""
            select r
            from Restaurant r
            join fetch r.chain
            where r.restaurantId = :restaurantId
              and r.isDeleted = false
            """)
    Optional<Restaurant> findDetailByRestaurantId(@Param("restaurantId") Long restaurantId);
}
