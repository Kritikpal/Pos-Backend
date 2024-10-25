package com.kritik.POS.restaurant.DAO;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class ProductFile {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long imageId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private LocalDateTime uploadTime = LocalDateTime.now();


}
