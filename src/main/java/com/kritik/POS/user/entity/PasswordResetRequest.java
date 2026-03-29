package com.kritik.POS.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_password_reset_request", indexes = {
        @Index(columnList = "email"),
        @Index(columnList = "tokenId"),
        @Index(columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String tokenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordResetStatus status; // PENDING, VERIFIED, EXPIRED, USED

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private LocalDateTime verifiedAt;

    private LocalDateTime usedAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isValid() {
        return status == PasswordResetStatus.PENDING && !isExpired();
    }
}
