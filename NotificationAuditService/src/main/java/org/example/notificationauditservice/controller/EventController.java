package org.example.notificationauditservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationauditservice.dto.EventRequest;
import org.example.notificationauditservice.service.EventService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @PostMapping
    public Mono<Void> process(
            @Valid @RequestBody EventRequest request,
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);

        if (!userId.equals(request.actorUserId())) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Event actor must match the authenticated user"
                    )
            );
        }

        EventRequest trustedRequest = new EventRequest(
                userId,
                authenticatedRole(authentication),
                request.action(),
                request.entityType(),
                request.entityId(),
                request.description(),
                request.notificationUserId(),
                request.notificationType(),
                request.notificationTitle(),
                request.notificationMessage()
        );

        return service.process(trustedRequest);
    }

    private Long authenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }

    private String authenticatedRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .map(authority -> authority.startsWith("ROLE_")
                        ? authority.substring(5)
                        : authority)
                .findFirst()
                .orElse("UNKNOWN");
    }
}