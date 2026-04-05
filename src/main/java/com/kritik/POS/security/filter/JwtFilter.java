package com.kritik.POS.security.filter;

import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.user.entity.RefreshToken;
import com.kritik.POS.user.repository.RefreshTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorizationHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
                String accessToken = authorizationHeader.substring(7);
                Claims claims = jwtUtil.extractAllClaims(accessToken);
                String tokenId = claims.get("tokenId", String.class);

                if (!isTokenActive(tokenId) || SecurityContextHolder.getContext().getAuthentication() != null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String username = claims.getSubject();
                Set<String> roles = extractRoles(claims);
                Long userId = claims.get("userId", Long.class);
                Long restaurantId = claims.get("restaurantId", Long.class);
                Long chainId = claims.get("chainId", Long.class);

                SecurityUser principal = new SecurityUser(
                        userId,
                        username,
                        accessToken,
                        tokenId,
                        restaurantId,
                        chainId,
                        roles
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                accessToken,
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

    private boolean isTokenActive(String tokenId) {
        if (!StringUtils.hasText(tokenId)) {
            return false;
        }
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenIdAndRevokedFalse(tokenId);
        return refreshToken
                .filter(token -> token.getExpiryDate() != null && token.getExpiryDate().isAfter(LocalDateTime.now()))
                .isPresent();
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
