package com.kritik.POS.configuredmenu.repository;

import com.kritik.POS.configuredmenu.entity.ConfiguredMenuTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConfiguredMenuTemplateRepository extends JpaRepository<ConfiguredMenuTemplate, Long> {

    Optional<ConfiguredMenuTemplate> findByParentMenuItem_IdAndIsDeletedFalse(Long parentMenuItemId);

    boolean existsByParentMenuItem_IdAndIsDeletedFalse(Long parentMenuItemId);

    @Query("""
            select t
            from ConfiguredMenuTemplate t
            join t.parentMenuItem p
            where t.isDeleted = false
              and (:skipRestaurantFilter = true or t.restaurantId in :restaurantIds)
              and (:isActive is null or t.isActive = :isActive)
              and (
                  coalesce(:search, '') = ''
                  or lower(p.itemName) like lower(concat('%', :search, '%'))
              )
            order by p.itemName asc, t.id asc
            """)
    List<ConfiguredMenuTemplate> findVisibleTemplates(@Param("skipRestaurantFilter") boolean skipRestaurantFilter,
                                                      @Param("restaurantIds") Collection<Long> restaurantIds,
                                                      @Param("isActive") Boolean isActive,
                                                      @Param("search") String search);
}
