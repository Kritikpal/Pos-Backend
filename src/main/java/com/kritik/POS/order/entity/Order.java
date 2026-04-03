package com.kritik.POS.order.entity;

import com.kritik.POS.order.entity.enums.PaymentStatus;
import com.kritik.POS.order.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<SaleItem> orderItemList;

    @OneToMany(mappedBy = "order",fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTax> orderTaxes;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false, updatable = false, unique = true)
    private String orderId;

    @Enumerated
    @Column(nullable = false)
    private PaymentType paymentType = PaymentType.UPI;

    @Enumerated
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAYMENT_INITIATED;

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

}
