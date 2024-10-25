package com.kritik.POS.restaurant.DAO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(indexes = {@Index(columnList = "categoryName")})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long categoryId;

    @Column(nullable = false,unique = true)
    private String categoryName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String categoryDescription;

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<MenuItem> menuItems;

    @Column(nullable = false)
    private Boolean isActive = true;

}
