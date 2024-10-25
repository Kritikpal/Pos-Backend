package com.kritik.POS.restaurant.DAO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ItemPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long priceId;

    @Column(nullable = false)
    private Double price = 0.0;

    @Column(nullable = false)
    private Double disCount = 0.0;

    @OneToOne(mappedBy = "itemPrice")
    @JsonIgnore
    private MenuItem menuItem;

}
