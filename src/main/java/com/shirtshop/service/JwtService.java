package com.shirtshop.service;

import com.shirtshop.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.access-secret}")
    private String accessSecret;

    @Value("${app.jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private Key keyFrom(String secret) {
        byte[] bytes = secret.getBytes();
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret is too short. Provide at least 256-bit key.");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getId())
                .claim("email", user.getEmail())
                .claim("displayName", user.getDisplayName())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + accessExpirationMs))
                .signWith(keyFrom(accessSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(user.getId())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + refreshExpirationMs))
                .signWith(keyFrom(refreshSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token, boolean isRefresh) {
        Key key = Keys.hmacShaKeyFor((isRefresh ? refreshSecret : accessSecret).getBytes());
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    // เผื่ออยากเรียกแบบชัดเจน
    public Jws<Claims> parseAccess(String token)  { return parse(token, false); }
    public Jws<Claims> parseRefresh(String token) { return parse(token, true); }
}
