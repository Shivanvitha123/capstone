package org.example.underwritingpolicyservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class PolicyEventPublisher {

    private final WebClient webClient;
    private final SecretKey secretKey;
    private final long jwtExpiration;

    public PolicyEventPublisher(
            WebClient.Builder webClientBuilder,
            @Value("${notification-audit.service-url:http://localhost:8086}")
            String serviceUrl,
            @Value("${jwt.secret}")
            String jwtSecret,
            @Value("${jwt.expiration:3600000}")
            long jwtExpiration
    ) {
        this.webClient = webClientBuilder
                .baseUrl(serviceUrl)
                .build();

        this.secretKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );

        this.jwtExpiration = jwtExpiration;
    }

    public Mono<Void> publish(
            Long actorUserId,
            String actorRole,
            String action,
            String entityType,
            Long entityId,
            String description,
            Long notificationUserId,
            String notificationType,
            String notificationTitle,
            String notificationMessage
    ) {
        if (actorUserId == null || notificationUserId == null) {
            return Mono.empty();
        }

        EventRequest request = new EventRequest(
                actorUserId,
                normalizeRole(actorRole),
                action,
                entityType,
                entityId,
                description,
                notificationUserId,
                notificationType,
                notificationTitle,
                notificationMessage
        );

        String token = createToken(actorUserId, actorRole);

        return webClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers ->
                        headers.setBearerAuth(token)
                )
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(error -> {
                    // Notification/audit failures must not break
                    // successful policy operations.
                    return Mono.empty();
                });
    }

    private String createToken(Long userId, String role) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", normalizeRole(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(
                        now.plusMillis(jwtExpiration)
                ))
                .signWith(secretKey)
                .compact();
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        return role.replaceFirst("^ROLE_", "");
    }

    private record EventRequest(
            Long actorUserId,
            String actorRole,
            String action,
            String entityType,
            Long entityId,
            String description,
            Long notificationUserId,
            String notificationType,
            String notificationTitle,
            String notificationMessage
    ) {
    }
}