package com.kritik.POS.restaurant.DAO;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ItemStock {
    @Id
    private String sku;

    @OneToOne
    @JoinColumn(nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer totalStock;
}
