package com.kritik.POS.user.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity()
@Table(name = "user_roles")
@Data
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue
    private Long roleId;

    @Column(unique = true)
    private String roleName;
}