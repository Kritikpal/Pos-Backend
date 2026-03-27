package com.kritik.POS.user.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Set<String> roles;
    private Long restaurantId;
    private Long chainId;
}
