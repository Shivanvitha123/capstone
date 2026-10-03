package org.example.riskintelligenceservice.integration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class NotificationAuditPublisher {

    private final WebClient webClient;
    private final SecretKey secretKey;
    private final long expiration;

    public NotificationAuditPublisher(
            WebClient.Builder webClientBuilder,
            @Value("${notification-audit.service-url:http://localhost:8086}") String serviceUrl,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long expiration) {
        this.webClient = webClientBuilder.baseUrl(serviceUrl).build();
        this.secretKey = Keys.hmacShaKeyFor(
                jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
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
            String notificationMessage) {

        if (actorUserId == null || notificationUserId == null) {
            return Mono.empty();
        }

        EventRequest request = new EventRequest(
                actorUserId,
                actorRole,
                action,
                entityType,
                entityId,
                description,
                notificationUserId,
                notificationType,
                notificationTitle,
                notificationMessage
        );

        return webClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(createServiceToken(actorUserId, actorRole)))
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(error -> {
                    // Notifications/audit must not undo a successful risk operation.
                    return Mono.empty();
                });
    }

    private String createServiceToken(Long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expiration)))
                .signWith(secretKey)
                .compact();
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
            String notificationMessage) {
    }
}
