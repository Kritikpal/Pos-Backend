package com.kritik.POS.admin.views.entity.compositKeys;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;


@Embeddable
@Data
public class MostOrderedMenuId implements Serializable {
    private String saleItemName;
    private Long restaurantId;
    private Integer paymentStatus;
    private LocalDate paymentDate;
}