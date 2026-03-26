package com.kritik.POS.security.filter;

import com.kritik.POS.exception.errors.AppAuthenticationException;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.entryPoint.JWTEntryPoint;
import com.kritik.POS.security.models.SecurityUser;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.security.util.SecurityUtil;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtFilter() {
        this.jwtUtil = new JwtUtil();
    }



    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = request.getHeader("Authorization");
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                Claims claims = jwtUtil.extractAllClaims(token);

                String username = claims.getSubject();
                Set<String> roles = claims.get("roles", Set.class);
                Long restaurantId = claims.get("restaurantId", Long.class);
                Long chainId = claims.get("chainId", Long.class);

                SecurityUser principal = new SecurityUser(
                        username,
                        restaurantId,
                        chainId,
                        roles, token
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal, token, principal.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        filterChain.doFilter(request, response);
    }



}
