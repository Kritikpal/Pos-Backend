package com.kritik.POS.admin.views.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;

@Entity
@Immutable // Hibernate annotation (important)
@Table(name = "mv_hourly_payment_summary")
@Data
public class HourlyPaymentView {

    @Id
    private String id; 
    // we'll create a synthetic id → required by JPA

    private Long restaurantId;

    private Integer paymentStatus; // ordinal

    private LocalDate paymentDate;

    private Integer hourOfDay;

    private Long totalOrders;

    private Double totalRevenue;
}