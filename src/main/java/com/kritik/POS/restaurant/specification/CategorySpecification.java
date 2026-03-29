package com.kritik.POS.restaurant.specification;

import com.kritik.POS.restaurant.entity.Category;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class CategorySpecification {

    private CategorySpecification() {
    }

    public static Specification<Category> hasId(Long id) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryId"), id);
    }

    public static Specification<Category> belongsToRestaurants(Collection<Long> restaurantIds) {
        return (root, query, criteriaBuilder) -> root.get("restaurantId").in(restaurantIds);
    }

    public static Specification<Category> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isFalse(root.get("isDeleted"));
    }
}
