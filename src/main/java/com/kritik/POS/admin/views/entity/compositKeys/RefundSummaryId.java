package com.kritik.POS.admin.views.entity.compositKeys;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
public class RefundSummaryId implements Serializable {

    private Long restaurantId;
    private LocalDate orderDate;
}