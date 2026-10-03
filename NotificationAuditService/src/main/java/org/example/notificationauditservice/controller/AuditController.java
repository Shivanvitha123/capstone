package org.example.notificationauditservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.notificationauditservice.dto.AuditResponse;
import org.example.notificationauditservice.dto.CreateAuditRequest;
import org.example.notificationauditservice.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService service;

    @PostMapping
    public Mono<AuditResponse> create(
            @Valid @RequestBody CreateAuditRequest request,
            Authentication authentication
    ) {
        Long userId = authenticatedUserId(authentication);

        if (!userId.equals(request.actorUserId())) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Actor ID must match the authenticated user"
                    )
            );
        }

        String role = authenticatedRole(authentication);

        CreateAuditRequest trustedRequest = new CreateAuditRequest(
                userId,
                role,
                request.action(),
                request.entityType(),
                request.entityId(),
                request.description()
        );

        return service.create(trustedRequest);
    }

    @GetMapping
    public Flux<AuditResponse> getAll(
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Flux.error(
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authentication required"
                    )
            );
        }

        if (!isAdmin(authentication)) {
            return Flux.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Only administrators can access audit logs"
                    )
            );
        }

        return service.getAll();
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

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .anyMatch(authority ->
                        "ADMIN".equalsIgnoreCase(authority)
                                || "ROLE_ADMIN".equalsIgnoreCase(authority)
                );
    }
}