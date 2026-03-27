package com.kritik.POS.user.repository;

import com.kritik.POS.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    @Query("SELECT u.userId as userId, u.email as email, u.phoneNumber as phoneNumber, u.chainId as chainId, u.restaurantId as restaurantId " +
           "FROM User u " +
           "WHERE (:chainId IS NULL OR u.chainId = :chainId) " +
           "AND (:restaurantId IS NULL OR u.restaurantId = :restaurantId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<com.kritik.POS.user.model.response.UserProjection> findUsers(
            @Param("chainId") Long chainId,
            @Param("restaurantId") Long restaurantId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT u.userId as userId, u.email as email, u.phoneNumber as phoneNumber, u.chainId as chainId, u.restaurantId as restaurantId " +
           "FROM User u JOIN u.roles r " +
           "WHERE r.roleName = :roleName " +
           "AND (:chainId IS NULL OR u.chainId = :chainId) " +
           "AND (:restaurantId IS NULL OR u.restaurantId = :restaurantId) " +
           "AND (:search IS NULL OR :search = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))" )
    Page<com.kritik.POS.user.model.response.UserProjection> findUsersByRole(
            @Param("roleName") String roleName,
            @Param("chainId") Long chainId,
            @Param("restaurantId") Long restaurantId,
            @Param("search") String search,
            Pageable pageable);

}
