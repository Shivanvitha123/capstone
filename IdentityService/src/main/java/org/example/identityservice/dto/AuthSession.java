package org.example.identityservice.dto;

public record AuthSession(
        String token,
        AuthResponse user
) {
}