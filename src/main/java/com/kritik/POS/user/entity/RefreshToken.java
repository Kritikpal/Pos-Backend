package com.kritik.POS.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenId; // UUID

    @Column(nullable = false)
    private Long userId;

    private boolean revoked = false;

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;
}