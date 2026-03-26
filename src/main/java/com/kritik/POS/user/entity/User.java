package com.kritik.POS.user.entity;


import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Data
@Entity
@Table(
        name = "tbl_user",
        indexes = {
                @Index(columnList = "email")
        })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long userId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 10, nullable = false)
    private String phoneNumber;


    @ManyToMany(fetch = FetchType.EAGER)
    private Set<Role> roles;


    // ✅ ONLY IDs (decoupled)
    private Long chainId;
    private Long restaurantId;
}
