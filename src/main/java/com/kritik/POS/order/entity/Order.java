package com.kritik.POS.order.entity;

import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Data
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_order_id", columnList = "orderId", unique = true),
        @Index(name = "idx_order_restaurant_last_updated", columnList = "restaurant_id,last_updated_time")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentInitiatedTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastUpdatedTime = LocalDateTime.now();

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();


    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<SaleItem> orderItemList = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderTaxSummary> orderTaxSummaries = new ArrayList<>();

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderLineTax> orderLineTaxes = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private OrderTaxContext orderTaxContext;

    @Deprecated
    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<OrderTax> orderTaxes = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "INR";

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "rounding_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal roundingAmount = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false, unique = true)
    private String orderId;

    @Enumerated
    @Column(nullable = false)
    private PaymentType paymentType = PaymentType.UPI;

    @Enumerated
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAYMENT_INITIATED;

    @Column(name = "payment_completed_at")
    private LocalDateTime paymentCompletedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payment_collected_by")
    private String paymentCollectedBy;

    @Column(name = "payment_notes", length = 1000)
    private String paymentNotes;

    @Column(name = "external_txn_id")
    private String externalTxnId;

    @Column(name = "operator_user_id")
    private Long operatorUserId;

    @Column(name = "refund_operator_user_id")
    private Long refundOperatorUserId;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "refund_notes", length = 1000)
    private String refundNotes;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (paymentInitiatedTime == null) {
            paymentInitiatedTime = LocalDateTime.now();
        }
        if (lastUpdatedTime == null) {
            lastUpdatedTime = paymentInitiatedTime;
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        lastUpdatedTime = updatedAt;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Order order = (Order) o;
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public final int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

}
