package com.kritik.POS.user.service.impl;

import com.kritik.POS.exception.errors.AppException;
import com.kritik.POS.security.util.JwtUtil;
import com.kritik.POS.user.entity.RefreshToken;
import com.kritik.POS.user.entity.Role;
import com.kritik.POS.user.entity.User;
import com.kritik.POS.user.model.request.LoginRequest;
import com.kritik.POS.user.model.response.LoginResponse;
import com.kritik.POS.user.repository.RefreshTokenRepository;
import com.kritik.POS.user.repository.UserRepository;
import com.kritik.POS.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public LoginResponse login(LoginRequest loginRequest) throws AppException {

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new AppException("This email is not registered", HttpStatus.BAD_REQUEST));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AppException("Username or password is not matching", HttpStatus.BAD_REQUEST);
        }

        String tokenId = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(tokenId)
                .userId(user.getUserId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        JwtUtil jwtUtil = new JwtUtil();
        Set<String> roles = user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        String primaryRole = roles.stream().findFirst().orElse("STAFF");

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", primaryRole);
        claims.put("roles", roles);
        claims.put("restaurantId", user.getRestaurantId());
        claims.put("chainId", user.getChainId());
        claims.put("tokenId", tokenId);

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                claims,
                System.currentTimeMillis() + 15 * 60 * 1000
        );

        String refreshTokenStr = jwtUtil.generateToken(
                user.getEmail(),
                Map.of("tokenId", tokenId),
                System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        );

        return new LoginResponse(
                accessToken,
                refreshTokenStr,
                primaryRole,
                roles,
                user.getRestaurantId(),
                user.getChainId()
        );
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {

        JwtUtil jwtUtil = new JwtUtil();
        String tokenId = jwtUtil.extractClaim(refreshToken, claims -> claims.get("tokenId", String.class));

        RefreshToken storedToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (storedToken.isRevoked()) {
            throw new RuntimeException("Token already revoked");
        }

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newTokenId = UUID.randomUUID().toString();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .tokenId(newTokenId)
                .userId(user.getUserId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        refreshTokenRepository.save(newRefreshToken);

        Set<String> roles = user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toSet());
        String primaryRole = roles.stream().findFirst().orElse("STAFF");
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", primaryRole);
        claims.put("roles", roles);
        claims.put("restaurantId", user.getRestaurantId());
        claims.put("chainId", user.getChainId());
        claims.put("tokenId", newTokenId);

        String accessToken = jwtUtil.generateToken(
                user.getEmail(),
                claims,
                System.currentTimeMillis() + 15 * 60 * 1000
        );

        String refreshTokenStr = jwtUtil.generateToken(
                user.getEmail(),
                Map.of("tokenId", newTokenId),
                System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        );

        return new LoginResponse(
                accessToken,
                refreshTokenStr,
                primaryRole,
                roles,
                user.getRestaurantId(),
                user.getChainId()
        );
    }

    @Override
    public User getUserByUserName(String email) throws AppException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.BAD_REQUEST));
    }
}
