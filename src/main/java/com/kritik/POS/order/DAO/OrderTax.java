package com.kritik.POS.order.DAO;

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
    @JoinColumn(unique = true,nullable = false,updatable = false)
    private Order order;
}
