package com.kritik.POS.admin.views.entity;

import com.kritik.POS.admin.views.entity.compositKeys.RefundSummaryId;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "mv_refund_summary")
@Immutable
@Data
public class RefundSummary {

    @EmbeddedId
    private RefundSummaryId id;

    private Long successfulOrders;

    private BigDecimal totalRevenue;

    private Long refundCount;

    private BigDecimal refundAmount;

    private BigDecimal refundPercentage;
}