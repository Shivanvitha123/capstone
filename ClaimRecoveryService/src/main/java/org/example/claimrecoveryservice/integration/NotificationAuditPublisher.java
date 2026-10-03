package org.example.claimrecoveryservice.integration;

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
    private final long expirationMillis;

    public NotificationAuditPublisher(
            WebClient.Builder builder,
            @Value("${notification-audit.service-url:http://localhost:8086}") String serviceUrl,
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration:3600000}") long expirationMillis) {
        this.webClient = builder.baseUrl(serviceUrl).build();
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    public Mono<Void> publish(Long actorUserId, String actorRole,
                              String action, String entityType, Long entityId,
                              Long recipientUserId, String title, String message,
                              String description) {
        if (actorUserId == null || recipientUserId == null) {
            return Mono.empty();
        }

        String token = Jwts.builder()
                .subject(actorUserId.toString())
                .claim("role", actorRole)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(expirationMillis)))
                .signWith(secretKey)
                .compact();

        EventRequest request = new EventRequest(
                actorUserId, actorRole, action, entityType, entityId,
                description, recipientUserId, "INFO", title, message);

        return webClient.post()
                .uri("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> headers.setBearerAuth(token))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> Mono.empty());
    }

    private record EventRequest(Long actorUserId, String actorRole,
                                String action, String entityType, Long entityId,
                                String description, Long notificationUserId,
                                String notificationType, String notificationTitle,
                                String notificationMessage) { }
}
