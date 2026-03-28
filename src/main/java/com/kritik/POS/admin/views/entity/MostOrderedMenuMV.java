package com.kritik.POS.admin.views.entity;

import com.kritik.POS.admin.views.entity.compositKeys.MostOrderedMenuId;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable
@Data
@Table(name = "mv_most_ordered_menu")
public class MostOrderedMenuMV {

    @EmbeddedId
    private MostOrderedMenuId mostOrderedMenuId;


    @Column(name = "total_quantity")
    private Long totalQuantity;

    @Column(name = "total_revenue")
    private Double totalRevenue;

}