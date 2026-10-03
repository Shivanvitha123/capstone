package org.example.notificationauditservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationauditservice.dto.CreateNotificationRequest;
import org.example.notificationauditservice.dto.NotificationResponse;
import org.example.notificationauditservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    public Mono<NotificationResponse> create(
            @Valid @RequestBody CreateNotificationRequest request,
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);

        if (!userId.equals(request.recipientUserId())) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "You can only create notifications for yourself"
                    )
            );
        }

        return service.create(request);
    }

    @GetMapping
    public Flux<NotificationResponse> getMine(
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);
        return service.getForUser(userId);
    }

    @GetMapping("/unread")
    public Flux<NotificationResponse> getUnread(
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);
        return service.getUnread(userId);
    }

    @PatchMapping("/{id}/read")
    public Mono<NotificationResponse> markRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);
        return service.markRead(id, userId);
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
}