package com.kritik.POS.security.models;

import java.util.Collection;
import java.util.Set;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
public class SecurityUser implements UserDetails {

    private final String email;
    private final String token;
    private final String tokenId;
    private final Long restaurantId;
    private final Long chainId;
    private final Set<String> roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return token;
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
        return roles.stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
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
