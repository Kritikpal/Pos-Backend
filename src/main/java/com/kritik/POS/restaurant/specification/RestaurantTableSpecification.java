package com.kritik.POS.restaurant.specification;

import com.kritik.POS.restaurant.entity.RestaurantTable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class RestaurantTableSpecification {

    private RestaurantTableSpecification() {
    }

    public static Specification<RestaurantTable> hasId(Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tableId"), id);
    }

    public static Specification<RestaurantTable> belongsToRestaurants(Collection<Long> restaurantIds) {
        return (root, query, criteriaBuilder) -> root.get("restaurantId").in(restaurantIds);
    }

    public static Specification<RestaurantTable> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("isDeleted"));
    }
}
