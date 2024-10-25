package com.kritik.POS.restaurant.DAO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kritik.POS.order.DAO.SaleItem;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(indexes = {@Index(columnList = "itemName")})
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String itemName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JoinColumn(nullable = false, name = "priceId")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ItemPrice itemPrice;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "categoryId", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isTrending = false;

    @OneToMany(mappedBy = "menuItem")
    @JsonIgnore
    private List<SaleItem> saleItems;

    @OneToOne(mappedBy = "menuItem",cascade = CascadeType.ALL,orphanRemoval = true)
    @JoinColumn(nullable = false)
    private ItemStock itemStock;

    @OneToOne
    @JoinColumn
    private ProductFile productImage;

}
