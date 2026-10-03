package org.example.businessservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class BusinessEventPublisher {

    private final WebClient webClient;

    public BusinessEventPublisher(
            WebClient.Builder webClientBuilder,
            @Value("${notification-audit.service-url:http://localhost:8086}")
            String notificationAuditServiceUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(notificationAuditServiceUrl)
                .build();
    }

    public Mono<Void> publish(
            BusinessEventRequest event,
            String authorization
    ) {
        if (authorization == null || authorization.isBlank()) {
            log.warn(
                    "Skipping event {} because no JWT was available",
                    event.action()
            );
            return Mono.empty();
        }

        String bearerToken = authorization.startsWith("Bearer ")
                ? authorization
                : "Bearer " + authorization;

        return webClient.post()
                .uri("/api/events")
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .bodyValue(event)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response ->
                        log.info(
                                "Published business event: {}",
                                event.action()
                        )
                )
                .doOnError(error ->
                        log.error(
                                "Failed to publish business event: {}",
                                event.action(),
                                error
                        )
                )
                .onErrorResume(error -> Mono.empty())
                .then();
    }

    public record BusinessEventRequest(
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