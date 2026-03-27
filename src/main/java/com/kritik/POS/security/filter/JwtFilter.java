package com.kritik.POS.security.filter;

import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter() {
        this.jwtUtil = new JwtUtil();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = request.getHeader("Authorization");
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                Claims claims = jwtUtil.extractAllClaims(token.substring(7));

                String username = claims.getSubject();
                Set<String> roles = extractRoles(claims);
                Long restaurantId = claims.get("restaurantId", Long.class);
                Long chainId = claims.get("chainId", Long.class);

                SecurityUser principal = new SecurityUser(
                        username,
                        restaurantId,
                        chainId,
                        roles,
                        token
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                token,
                                principal.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private Set<String> extractRoles(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim instanceof Collection<?> collection) {
            Set<String> roles = new LinkedHashSet<>();
            collection.forEach(role -> roles.add(String.valueOf(role)));
            return roles;
        }
        String role = claims.get("role", String.class);
        if (role != null && !role.isBlank()) {
            return Set.of(role);
        }
        return Set.of();
    }
}
