package com.kritik.POS.user.DAO;


import com.kritik.POS.user.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(indexes = {
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

    @Enumerated
    @Column(nullable = false)
    private UserRole userRole = UserRole.STAFF;


}
