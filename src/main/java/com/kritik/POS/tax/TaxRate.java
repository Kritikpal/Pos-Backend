package com.kritik.POS.tax;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class TaxRate {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long taxId;

    @Column(nullable = false,unique = true)
    private String taxName;

    @Column(nullable = false)
    private Double taxAmount;

    @Column(nullable = false)
    private boolean isActive = true;

}
