package com.kritik.POS.mobile.repository;

import com.kritik.POS.mobile.repository.row.CategorySyncRow;
import com.kritik.POS.mobile.repository.row.IngredientStockSyncRow;
import com.kritik.POS.mobile.repository.row.ItemStockSyncRow;
import com.kritik.POS.mobile.repository.row.MenuItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuPriceSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeItemSyncRow;
import com.kritik.POS.mobile.repository.row.MenuRecipeSyncRow;
import com.kritik.POS.mobile.repository.row.PosSettingSyncRow;
import com.kritik.POS.mobile.repository.row.PreparedStockSyncRow;
import com.kritik.POS.mobile.repository.row.TaxClassSyncRow;
import com.kritik.POS.mobile.repository.row.TaxConfigSyncRow;
import com.kritik.POS.mobile.repository.row.TaxDefinitionSyncRow;
import com.kritik.POS.mobile.repository.row.TaxRegistrationSyncRow;
import com.kritik.POS.mobile.repository.row.TaxRuleSyncRow;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PosSyncRepository extends Repository<com.kritik.POS.restaurant.entity.Restaurant, Long> {

    @Query("""
            select new com.kritik.POS.mobile.repository.row.CategorySyncRow(
                c.categoryId,
                c.restaurantId,
                c.categoryName,
                c.categoryDescription,
                c.isActive,
                c.isDeleted,
                c.createdAt,
                c.updatedAt,
                coalesce(c.updatedAt, c.createdAt)
            )
            from Category c
            where c.restaurantId = :restaurantId
              and (
                    coalesce(c.updatedAt, c.createdAt) > :lastSyncTime
                    or (
                        coalesce(c.updatedAt, c.createdAt) = :lastSyncTime
                        and c.categoryId > :lastSeenId
                    )
              )
            order by coalesce(c.updatedAt, c.createdAt) asc, c.categoryId asc
            """)
    List<CategorySyncRow> findCategoryRows(@Param("restaurantId") Long restaurantId,
                                           @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                           @Param("lastSeenId") Long lastSeenId,
                                           Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.MenuItemSyncRow(
                m.id,
                m.restaurantId,
                c.categoryId,
                ip.priceId,
                m.taxClassId,
                pf.url,
                m.itemName,
                m.description,
                m.isAvailable,
                m.isActive,
                m.isTrending,
                m.menuType,
                m.isDeleted,
                m.createdAt,
                m.updatedAt,
                coalesce(m.updatedAt, m.createdAt)
            )
            from MenuItem m
            join m.category c
            left join m.itemPrice ip
            left join m.productImage pf
            where m.restaurantId = :restaurantId
              and (
                    coalesce(m.updatedAt, m.createdAt) > :lastSyncTime
                    or (
                        coalesce(m.updatedAt, m.createdAt) = :lastSyncTime
                        and m.id > :lastSeenId
                    )
              )
            order by coalesce(m.updatedAt, m.createdAt) asc, m.id asc
            """)
    List<MenuItemSyncRow> findMenuItemRows(@Param("restaurantId") Long restaurantId,
                                           @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                           @Param("lastSeenId") Long lastSeenId,
                                           Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.MenuPriceSyncRow(
                ip.priceId,
                m.id,
                m.restaurantId,
                ip.price,
                ip.disCount,
                ip.priceIncludesTax,
                coalesce(m.updatedAt, m.createdAt)
            )
            from MenuItem m
            join m.itemPrice ip
            where m.restaurantId = :restaurantId
              and m.isDeleted = false
              and (
                    coalesce(m.updatedAt, m.createdAt) > :lastSyncTime
                    or (
                        coalesce(m.updatedAt, m.createdAt) = :lastSyncTime
                        and ip.priceId > :lastSeenId
                    )
              )
            order by coalesce(m.updatedAt, m.createdAt) asc, ip.priceId asc
            """)
    List<MenuPriceSyncRow> findPriceRows(@Param("restaurantId") Long restaurantId,
                                         @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                         @Param("lastSeenId") Long lastSeenId,
                                         Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.TaxConfigSyncRow(
                t.id,
                t.restaurantId,
                t.displayName,
                t.defaultValue,
                t.isActive,
                t.isDeleted,
                t.createdAt,
                t.updatedAt,
                coalesce(t.updatedAt, t.createdAt)
            )
            from TaxDefinition t
            where t.restaurantId = :restaurantId
              and (
                    coalesce(t.updatedAt, t.createdAt) > :lastSyncTime
                    or (
                        coalesce(t.updatedAt, t.createdAt) = :lastSyncTime
                        and t.id > :lastSeenId
                    )
              )
            order by coalesce(t.updatedAt, t.createdAt) asc, t.id asc
            """)
    List<TaxConfigSyncRow> findTaxRows(@Param("restaurantId") Long restaurantId,
                                       @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                       @Param("lastSeenId") Long lastSeenId,
                                       Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.TaxClassSyncRow(
                t.id,
                t.restaurantId,
                t.code,
                t.name,
                t.description,
                t.isExempt,
                t.isActive,
                t.isDeleted,
                t.createdAt,
                t.updatedAt,
                coalesce(t.updatedAt, t.createdAt)
            )
            from TaxClass t
            where t.restaurantId = :restaurantId
              and (
                    coalesce(t.updatedAt, t.createdAt) > :lastSyncTime
                    or (
                        coalesce(t.updatedAt, t.createdAt) = :lastSyncTime
                        and t.id > :lastSeenId
                    )
              )
            order by coalesce(t.updatedAt, t.createdAt) asc, t.id asc
            """)
    List<TaxClassSyncRow> findTaxClassRows(@Param("restaurantId") Long restaurantId,
                                           @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                           @Param("lastSeenId") Long lastSeenId,
                                           Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.TaxDefinitionSyncRow(
                t.id,
                t.restaurantId,
                t.code,
                t.displayName,
                t.kind,
                t.valueType,
                t.defaultValue,
                t.currencyCode,
                t.isRecoverable,
                t.isActive,
                t.isDeleted,
                t.createdAt,
                t.updatedAt,
                coalesce(t.updatedAt, t.createdAt)
            )
            from TaxDefinition t
            where t.restaurantId = :restaurantId
              and (
                    coalesce(t.updatedAt, t.createdAt) > :lastSyncTime
                    or (
                        coalesce(t.updatedAt, t.createdAt) = :lastSyncTime
                        and t.id > :lastSeenId
                    )
              )
            order by coalesce(t.updatedAt, t.createdAt) asc, t.id asc
            """)
    List<TaxDefinitionSyncRow> findTaxDefinitionRows(@Param("restaurantId") Long restaurantId,
                                                     @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                                     @Param("lastSeenId") Long lastSeenId,
                                                     Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.TaxRuleSyncRow(
                t.id,
                t.restaurantId,
                t.taxDefinitionId,
                t.taxClassId,
                t.calculationMode,
                t.compoundMode,
                t.sequenceNo,
                t.validFrom,
                t.validTo,
                t.countryCode,
                t.regionCode,
                t.buyerTaxCategory,
                t.minAmount,
                t.maxAmount,
                t.priority,
                t.isActive,
                t.isDeleted,
                t.createdAt,
                t.updatedAt,
                coalesce(t.updatedAt, t.createdAt)
            )
            from TaxRule t
            where t.restaurantId = :restaurantId
              and (
                    coalesce(t.updatedAt, t.createdAt) > :lastSyncTime
                    or (
                        coalesce(t.updatedAt, t.createdAt) = :lastSyncTime
                        and t.id > :lastSeenId
                    )
              )
            order by coalesce(t.updatedAt, t.createdAt) asc, t.id asc
            """)
    List<TaxRuleSyncRow> findTaxRuleRows(@Param("restaurantId") Long restaurantId,
                                         @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                         @Param("lastSeenId") Long lastSeenId,
                                         Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.TaxRegistrationSyncRow(
                t.id,
                t.restaurantId,
                t.schemeCode,
                t.registrationNumber,
                t.legalName,
                t.countryCode,
                t.regionCode,
                t.placeOfBusiness,
                t.isDefault,
                t.validFrom,
                t.validTo,
                t.isActive,
                t.createdAt,
                t.updatedAt,
                coalesce(t.updatedAt, t.createdAt)
            )
            from TaxRegistration t
            where t.restaurantId = :restaurantId
              and (
                    coalesce(t.updatedAt, t.createdAt) > :lastSyncTime
                    or (
                        coalesce(t.updatedAt, t.createdAt) = :lastSyncTime
                        and t.id > :lastSeenId
                    )
              )
            order by coalesce(t.updatedAt, t.createdAt) asc, t.id asc
            """)
    List<TaxRegistrationSyncRow> findTaxRegistrationRows(@Param("restaurantId") Long restaurantId,
                                                         @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                                         @Param("lastSeenId") Long lastSeenId,
                                                         Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.ItemStockSyncRow(
                s.sku,
                s.restaurantId,
                m.id,
                sup.supplierId,
                sup.supplierName,
                s.totalStock,
                s.reorderLevel,
                s.unitOfMeasure,
                m.baseUnit.id,
                coalesce(m.baseUnit.code, s.unitOfMeasure),
                s.isActive,
                s.isDeleted,
                s.lastRestockedAt,
                s.updatedAt,
                coalesce(s.updatedAt, s.createdAt)
            )
            from ItemStock s
            join s.menuItem m
            left join s.supplier sup
            where s.restaurantId = :restaurantId
              and (
                    coalesce(s.updatedAt, s.createdAt) > :lastSyncTime
                    or (
                        coalesce(s.updatedAt, s.createdAt) = :lastSyncTime
                        and s.sku > :lastSeenSku
                    )
              )
            order by coalesce(s.updatedAt, s.createdAt) asc, s.sku asc
            """)
    List<ItemStockSyncRow> findItemStockRows(@Param("restaurantId") Long restaurantId,
                                             @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                             @Param("lastSeenSku") String lastSeenSku,
                                             Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.IngredientStockSyncRow(
                i.sku,
                i.restaurantId,
                i.ingredientName,
                i.description,
                sup.supplierId,
                sup.supplierName,
                i.totalStock,
                i.reorderLevel,
                i.unitOfMeasure,
                i.ingredient.baseUnit.id,
                coalesce(i.ingredient.baseUnit.code, i.unitOfMeasure),
                i.isActive,
                i.isDeleted,
                i.lastRestockedAt,
                i.updatedAt,
                coalesce(i.updatedAt, i.createdAt)
            )
            from IngredientStock i
            left join i.supplier sup
            where i.restaurantId = :restaurantId
              and (
                    coalesce(i.updatedAt, i.createdAt) > :lastSyncTime
                    or (
                        coalesce(i.updatedAt, i.createdAt) = :lastSyncTime
                        and i.sku > :lastSeenSku
                    )
              )
            order by coalesce(i.updatedAt, i.createdAt) asc, i.sku asc
            """)
    List<IngredientStockSyncRow> findIngredientStockRows(@Param("restaurantId") Long restaurantId,
                                                         @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                                         @Param("lastSeenSku") String lastSeenSku,
                                                         Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.MenuRecipeSyncRow(
                r.id,
                m.id,
                m.restaurantId,
                r.batchSize,
                r.active,
                coalesce(m.updatedAt, m.createdAt)
            )
            from MenuRecipe r
            join r.menuItem m
            where m.restaurantId = :restaurantId
              and m.isDeleted = false
              and (
                    coalesce(m.updatedAt, m.createdAt) > :lastSyncTime
                    or (
                        coalesce(m.updatedAt, m.createdAt) = :lastSyncTime
                        and r.id > :lastSeenId
                    )
              )
            order by coalesce(m.updatedAt, m.createdAt) asc, r.id asc
            """)
    List<MenuRecipeSyncRow> findRecipeRows(@Param("restaurantId") Long restaurantId,
                                           @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                           @Param("lastSeenId") Long lastSeenId,
                                           Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.MenuRecipeItemSyncRow(
                mii.id,
                r.id,
                m.id,
                m.restaurantId,
                ingredient.sku,
                mii.quantityRequired,
                mii.createdAt,
                mii.updatedAt,
                coalesce(mii.updatedAt, mii.createdAt)
            )
            from MenuItemIngredient mii
            join mii.menuItem m
            left join mii.recipe r
            join mii.ingredientStock ingredient
            where m.restaurantId = :restaurantId
              and m.isDeleted = false
              and (
                    coalesce(mii.updatedAt, mii.createdAt) > :lastSyncTime
                    or (
                        coalesce(mii.updatedAt, mii.createdAt) = :lastSyncTime
                        and mii.id > :lastSeenId
                    )
              )
            order by coalesce(mii.updatedAt, mii.createdAt) asc, mii.id asc
            """)
    List<MenuRecipeItemSyncRow> findRecipeItemRows(@Param("restaurantId") Long restaurantId,
                                                   @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                                   @Param("lastSeenId") Long lastSeenId,
                                                   Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.PreparedStockSyncRow(
                ps.menuItemId,
                ps.restaurantId,
                ps.availableQty,
                ps.reservedQty,
                ps.unitCode,
                ps.active,
                ps.createdAt,
                ps.updatedAt,
                coalesce(ps.updatedAt, ps.createdAt)
            )
            from PreparedItemStock ps
            where ps.restaurantId = :restaurantId
              and (
                    coalesce(ps.updatedAt, ps.createdAt) > :lastSyncTime
                    or (
                        coalesce(ps.updatedAt, ps.createdAt) = :lastSyncTime
                        and ps.menuItemId > :lastSeenId
                    )
              )
            order by coalesce(ps.updatedAt, ps.createdAt) asc, ps.menuItemId asc
            """)
    List<PreparedStockSyncRow> findPreparedStockRows(@Param("restaurantId") Long restaurantId,
                                                     @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                                     @Param("lastSeenId") Long lastSeenId,
                                                     Pageable pageable);

    @Query("""
            select new com.kritik.POS.mobile.repository.row.PosSettingSyncRow(
                r.restaurantId,
                r.chainId,
                r.code,
                r.name,
                r.currency,
                r.timezone,
                r.gstNumber,
                r.phoneNumber,
                r.email,
                coalesce(r.updatedAt, r.createdAt)
            )
            from Restaurant r
            where r.restaurantId = :restaurantId
              and r.isDeleted = false
              and (
                    coalesce(r.updatedAt, r.createdAt) > :lastSyncTime
                    or (
                        coalesce(r.updatedAt, r.createdAt) = :lastSyncTime
                        and r.restaurantId > :lastSeenId
                    )
              )
            order by coalesce(r.updatedAt, r.createdAt) asc, r.restaurantId asc
            """)
    List<PosSettingSyncRow> findSettingRows(@Param("restaurantId") Long restaurantId,
                                            @Param("lastSyncTime") LocalDateTime lastSyncTime,
                                            @Param("lastSeenId") Long lastSeenId,
                                            Pageable pageable);
}
