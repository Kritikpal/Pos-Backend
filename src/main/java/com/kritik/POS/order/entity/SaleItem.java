package com.kritik.POS.order.entity;

import com.kritik.POS.restaurant.entity.MenuItem;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class SaleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long saleItemId;

    @Column(nullable = false,updatable = false)
    private String saleItemName;

    @Column(nullable = false, updatable = false)
    private Double saleItemPrice;

    @Column(nullable = false,updatable = false)
    private Integer amount;

    @ManyToOne
    @JoinColumn(nullable = false,updatable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "manu_itemId")
    private MenuItem menuItem;


}
