package com.kritik.POS.admin.views.entity;

import com.kritik.POS.admin.views.entity.compositKeys.DailyKpiId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "mv_daily_kpi")
@Data
@Immutable
public class DailyKpi {

    @EmbeddedId
    private DailyKpiId id;

    private Long totalOrders;

    private Long successfulOrders;

    private Long canceledOrders;

    private BigDecimal totalRevenue;

    private BigDecimal avgOrderValue;

    private Double successRate;
}