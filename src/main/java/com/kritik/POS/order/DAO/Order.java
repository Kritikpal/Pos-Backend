package com.kritik.POS.order.DAO;

import com.kritik.POS.order.DAO.enums.PaymentStatus;
import com.kritik.POS.order.DAO.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "orders", indexes = {
        @Index(columnList = "orderId", unique = true)
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime paymentInitiatedTime = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime lastUpdatedTime = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> orderItemList;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTax> orderTaxes;

    @Column(updatable = false, nullable = false)
    private Double totalPrice;

    @Column(nullable = false, updatable = false, unique = true)
    private String orderId;

    @Enumerated
    @Column(nullable = false)
    private PaymentType paymentType = PaymentType.UPI;

    @Enumerated
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PAYMENT_INITIATED;

}
