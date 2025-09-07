package com.shirtshop.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.*;

@Service
public class JwtService {

    private final Key accessKey;
    private final Key refreshKey;

    @Value("${app.jwt.access-expiration-ms:900000}") // default 15 นาที
    private long accessExpMs;

    @Value("${app.jwt.refresh-expiration-ms:604800000}") // default 7 วัน
    private long refreshExpMs;

    public JwtService(
            @Value("${app.jwt.access-secret}") String accessSecret,
            @Value("${app.jwt.refresh-secret}") String refreshSecret
    ) {
        this.accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes());
    }

    // ✅ generate access token
    public String genAccess(String userId, String email, String roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpMs);
        return Jwts.builder()
                .setSubject(userId)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(accessKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ generate refresh token
    public String genRefresh(String userId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpMs);
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ parse access token
    public Claims parseAccess(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(accessKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ parse refresh token
    public Claims parseRefresh(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(refreshKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ helper extract roles
    public List<String> extractRoles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw == null) return List.of();
        if (raw instanceof String s) {
            return Arrays.asList(s.split(","));
        }
        if (raw instanceof Collection<?> c) {
            return c.stream().map(Object::toString).toList();
        }
        return List.of(raw.toString());
    }
}
