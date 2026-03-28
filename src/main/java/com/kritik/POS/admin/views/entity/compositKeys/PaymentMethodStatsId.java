package com.kritik.POS.admin.views.entity.compositKeys;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodStatsId implements Serializable {

    private Long restaurantId;
    private LocalDate orderDate;
    private Long paymentType; // or paymentTypeId if you used int
}