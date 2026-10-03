package org.example.claimrecoveryservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.claimrecoveryservice.dto.CreateRecoveryRequest;
import org.example.claimrecoveryservice.dto.RecoveryResponse;
import org.example.claimrecoveryservice.dto.UpdateRecoveryStatusRequest;
import org.example.claimrecoveryservice.exception.ClaimAccessDeniedException;
import org.example.claimrecoveryservice.service.ClaimEventPublisher;
import org.example.claimrecoveryservice.service.RecoveryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class RecoveryController {

    private final RecoveryService service;
    private final ClaimEventPublisher eventPublisher;

    // =====================================================
    // CREATE RECOVERY
    // POST /api/claims/{claimId}/recovery
    // =====================================================

    @PostMapping("/{claimId}/recovery")
    public Mono<ResponseEntity<RecoveryResponse>> createRecovery(
            @PathVariable Long claimId,
            @Valid @RequestBody CreateRecoveryRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.createRecovery(
                        claimId,
                        request,
                        userId,
                        role
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "RECOVERY_CREATED",
                                "RECOVERY",
                                response.id(),
                                "Recovery record " + response.id()
                                        + " was created for claim "
                                        + response.claimId() + ".",
                                response.ownerId(),
                                "INFO",
                                "Recovery Created",
                                "A recovery record was created for claim "
                                        + response.claimId() + "."
                        ).thenReturn(
                                ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(response)
                        )
                );
    }

    // =====================================================
    // GET RECOVERIES FOR CLAIM
    // GET /api/claims/{claimId}/recovery
    // =====================================================

    @GetMapping("/{claimId}/recovery")
    public Flux<RecoveryResponse> getClaimRecoveries(
            @PathVariable Long claimId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getClaimRecoveries(
                claimId,
                userId,
                role
        );
    }

    // =====================================================
    // GET ALL RECOVERIES
    // GET /api/claims/recovery
    // =====================================================

    @GetMapping("/recovery")
    public Flux<RecoveryResponse> getAllRecoveries(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getAllRecoveries(userId, role);
    }

    // =====================================================
    // UPDATE RECOVERY STATUS
    // PATCH /api/claims/recovery/{recoveryId}/status
    // =====================================================

    @PatchMapping("/recovery/{recoveryId}/status")
    public Mono<RecoveryResponse> updateRecoveryStatus(
            @PathVariable Long recoveryId,
            @Valid @RequestBody UpdateRecoveryStatusRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.updateRecoveryStatus(
                        recoveryId,
                        request,
                        userId,
                        role
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "RECOVERY_STATUS_UPDATED",
                                "RECOVERY",
                                response.id(),
                                "Recovery record " + response.id()
                                        + " for claim "
                                        + response.claimId()
                                        + " status changed to "
                                        + response.status() + ".",
                                response.ownerId(),
                                "INFO",
                                "Recovery Status Updated",
                                "The recovery status for claim "
                                        + response.claimId()
                                        + " is now "
                                        + response.status() + "."
                        ).thenReturn(response)
                );
    }

    // =====================================================
    // AUTHENTICATION HELPERS
    // =====================================================

    private Long getUserId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            return null;
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String getRole(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        return authentication.getAuthorities()
                .stream()
                .map(authority -> normalizeRole(
                        authority.getAuthority()
                ))
                .filter(role -> !role.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized = role.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }
}