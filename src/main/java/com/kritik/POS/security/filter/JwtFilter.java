package com.kritik.POS.security.filter;

import com.kritik.POS.exception.errors.AppAuthenticationException;
import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.entryPoint.JWTEntryPoint;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.security.util.SecurityUtil;
import com.kritik.POS.user.DAO.User;
import com.kritik.POS.user.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final JWTEntryPoint jwtEntryPoint;

    public JwtFilter(UserService userService, JWTEntryPoint jwtEntryPoint) {
        this.jwtEntryPoint = jwtEntryPoint;
        this.jwtUtil = new JwtUtil();
        this.userService = userService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authToken = extractTokenFromHeader(request);
        if (authToken == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String userName = jwtUtil.getUserName(authToken);
            User user = userService.getUserByUserName(userName);
            SecurityUtil.setSecurityContent(user,request);
            filterChain.doFilter(request, response);
        } catch (JwtException | AppException e) {
            jwtEntryPoint.commence(request, response, new AppAuthenticationException("Invalid token"));
        }
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String requestHeader = request.getHeader("Authorization");
        if (requestHeader == null || !requestHeader.startsWith("Bearer ")) {
            return null;
        }
        return requestHeader.substring(7);
    }


}
