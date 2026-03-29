package com.kritik.POS.user.model.response;

import lombok.Data;

import java.util.Set;

@Data
public class UserResponse {
    private Long userId;
    private String email;
    private String phoneNumber;
    private Long chainId;
    private Long restaurantId;
    private Set<String> roles;
}
