package com.kritik.POS.security.util;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

public class JwtUtil {

    private static final String SECRET_KEY = "dskjdvslcbvbsjdcskjdhsgdaihsvdhhefy8377892u3esnbcnc";

    public String generateToken(String userName, Map<String ,Object> claims, Long expTimeInMilliSecond) {

        Date startTime = new Date(System.currentTimeMillis());
        SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .signWith(secretKey)
                .claims(claims)
                .issuer("Kritik Pal")
                .subject(userName)
                .issuedAt(startTime)
                .expiration(new Date(expTimeInMilliSecond))
                .compact();
    }

    private static Claims getClaims(String token) {
        try {
            SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            JwtParser parser = Jwts.parser()
                    .verifyWith(secretKey)
                    .build();
            Jws<Claims> claimsJws = parser.parseSignedClaims(token);
            return claimsJws.getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("Invalid JWT Token");
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaims(token);
        return claimsResolver.apply(claims);
    }


    public String getUserName(String token) throws JwtException {
        return getClaims(token).getSubject();
    }


}
