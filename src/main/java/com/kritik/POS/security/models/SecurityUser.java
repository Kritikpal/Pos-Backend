package com.kritik.POS.security.models;


import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SecurityUser implements UserDetails {

    private final String email;
    private final String password;
    private final Long restaurantId;
    private final Long chainId;
    private final Set<String> roles;


    public SecurityUser(String username, Long restaurantId, Long chainId, Set<String> roles, String token) {
        this.email = username;
        this.password = token;
        this.chainId = chainId;
        this.restaurantId = restaurantId;
        this.roles = roles;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.equalsIgnoreCase(roleName));
    }

    public boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    public boolean isChainAdmin() {
        return hasRole("CHAIN_ADMIN");
    }

    public boolean isRestaurantAdmin() {
        return hasRole("RESTAURANT_ADMIN");
    }

    public boolean isStaff() {
        return hasRole("STAFF");
    }
}
