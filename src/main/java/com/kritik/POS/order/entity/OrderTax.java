package com.kritik.POS.order.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class OrderTax {
    @Id
    @GeneratedValue
    private Long id;
    private String taxName;
    private Double taxAmount;
    @ManyToOne
    @JoinColumn(nullable = false,updatable = false)
    private Order order;
}
