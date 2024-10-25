package com.kritik.POS.security.models;

public class JWTClaim {

    private final SecurityUser securityUser;

    public JWTClaim(SecurityUser securityUser) {
        this.securityUser = securityUser;
    }


}
