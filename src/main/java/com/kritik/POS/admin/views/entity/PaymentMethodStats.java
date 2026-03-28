package com.kritik.POS.admin.views.entity;

import com.kritik.POS.admin.views.entity.compositKeys.PaymentMethodStatsId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "mv_payment_method_stats")
@Data
@Immutable
public class PaymentMethodStats {

    @EmbeddedId
    private PaymentMethodStatsId id;

    private Long totalOrders;

    private Long successfulOrders;

    private BigDecimal revenue;

    private Double successRate;
}