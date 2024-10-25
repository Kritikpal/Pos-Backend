package com.kritik.POS.order.DAO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kritik.POS.restaurant.DAO.MenuItem;
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
    @JsonIgnore
    private Order order;

    @ManyToOne
    @JoinColumn(name = "manu_itemId")
    @JsonIgnore
    private MenuItem menuItem;


}
