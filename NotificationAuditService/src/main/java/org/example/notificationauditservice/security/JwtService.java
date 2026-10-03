package org.example.notificationauditservice.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);

        Object subject = claims.getSubject();

        if (subject == null) {
            throw new RuntimeException("User ID not found in token");
        }

        return Long.valueOf(subject.toString());
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);

        Object role = claims.get("role");

        return role != null ? role.toString() : null;
    }

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);

        Object email = claims.get("email");

        return email != null ? email.toString() : null;
    }
}

