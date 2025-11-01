package com.thesis.backend.security;

import com.thesis.backend.entity.User;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    // Use a fixed secret key so tokens remain valid across restarts
    private final Key key = Keys.hmacShaKeyFor("MySecretJwtKeyForThesisApp123456789012345678901234567890123456789012345678".getBytes());

    public String generateToken(User user) {
        // 24 hours
        long EXPIRATION = 1000 * 60 * 60 * 24;
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT validation error: {}", e.getMessage());
            return false;
        }
    }
}
