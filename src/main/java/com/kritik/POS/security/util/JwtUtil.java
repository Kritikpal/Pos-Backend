package com.kritik.POS.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secretKeyValue;

    @Value("${app.jwt.issuer:pos-backend}")
    private String issuer;

    @Value("${app.jwt.access-seconds:900}")
    private long accessTokenSeconds;

    @Value("${app.jwt.refresh-seconds:604800}")
    private long refreshTokenSeconds;

    private SecretKey secretKey;
    private JwtParser jwtParser;

    @PostConstruct
    void init() {
        if (secretKeyValue == null || secretKeyValue.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }

        this.secretKey = Keys.hmacShaKeyFor(secretKeyValue.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parser()
                .requireIssuer(issuer)
                .verifyWith(secretKey)
                .build();
    }

    public String generateAccessToken(String userName, Map<String, Object> claims) {
        return generateToken(userName, claims, accessTokenSeconds);
    }

    public String generateRefreshToken(String userName, Map<String, Object> claims) {
        return generateToken(userName, claims, refreshTokenSeconds);
    }

    public String generateToken(String userName, Map<String, Object> claims, long expiresInSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .issuer(issuer)
                .subject(userName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(secretKey)
                .compact();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public Claims extractAllClaims(String token) {
        try {
            return jwtParser.parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException exception) {
            throw new JwtException("Invalid JWT token", exception);
        }
    }

    public String getUserName(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get("tokenId", String.class));
    }
}
