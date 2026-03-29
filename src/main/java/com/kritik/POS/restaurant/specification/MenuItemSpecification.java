package com.kritik.POS.restaurant.specification;

import com.kritik.POS.restaurant.entity.MenuItem;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class MenuItemSpecification {

    private MenuItemSpecification() {
    }

    public static Specification<MenuItem> hasId(Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("id"), id);
    }

    public static Specification<MenuItem> belongsToRestaurants(Collection<Long> restaurantIds) {
        return (root, query, criteriaBuilder) -> root.get("restaurantId").in(restaurantIds);
    }

    public static Specification<MenuItem> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("isDeleted"));
    }
}
