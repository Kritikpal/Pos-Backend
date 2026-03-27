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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Override
    public LoginResponse login(LoginRequest loginRequest) throws AppException {
        User user = userRepository.findByEmail(loginRequest.email().trim().toLowerCase())
                .orElseThrow(() -> new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(loginRequest.password(), user.getPassword())) {
            throw new AppException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }

        return issueTokens(user);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) throws AppException {
        String tokenId = jwtUtil.extractTokenId(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (storedToken.isRevoked()) {
            throw new AppException("Refresh token has already been revoked", HttpStatus.UNAUTHORIZED);
        }

        if (storedToken.getExpiryDate() == null || storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new AppException("Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.UNAUTHORIZED));

        return issueTokens(user);
    }

    @Override
    public void logout(String bearerToken, String refreshToken) throws AppException {
        String tokenId = resolveTokenId(bearerToken, refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new AppException("Session not found or already logged out", HttpStatus.NOT_FOUND));

        if (!storedToken.isRevoked()) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
        }
    }

    @Override
    public User getUserByUserName(String email) throws AppException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.BAD_REQUEST));
    }

    private LoginResponse issueTokens(User user) {
        String tokenId = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .tokenId(tokenId)
                .userId(user.getUserId())
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build());

        Set<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet());
        String primaryRole = roles.stream().findFirst().orElse("STAFF");

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", primaryRole);
        claims.put("roles", roles);
        claims.put("restaurantId", user.getRestaurantId());
        claims.put("chainId", user.getChainId());
        claims.put("tokenId", tokenId);

        return new LoginResponse(
                jwtUtil.generateAccessToken(user.getEmail(), claims),
                jwtUtil.generateRefreshToken(user.getEmail(), Map.of("tokenId", tokenId)),
                primaryRole,
                roles,
                user.getRestaurantId(),
                user.getChainId()
        );
    }

    private String resolveTokenId(String bearerToken, String refreshToken) {
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return jwtUtil.extractTokenId(bearerToken.substring(7));
        }
        if (StringUtils.hasText(refreshToken)) {
            return jwtUtil.extractTokenId(refreshToken);
        }
        throw new AppException("Provide a bearer token or refresh token to logout", HttpStatus.BAD_REQUEST);
    }
}
