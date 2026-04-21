package com.kritik.POS.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
public class ItemPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long priceId;

    @Column(nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal disCount = BigDecimal.ZERO;

    @Column(name = "price_includes_tax", nullable = false)
    private Boolean priceIncludesTax = false;

    @OneToOne(mappedBy = "itemPrice")
    @JsonIgnore
    private MenuItem menuItem;

}
