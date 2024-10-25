package com.kritik.POS.restaurant.DAO;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class RestaurantTable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long tableId;

    @Column(name = "table_number", unique = true, nullable = false)
    private Integer tableNumber = 1;

    @Column(nullable = false)
    private Integer seats = 1;


}
