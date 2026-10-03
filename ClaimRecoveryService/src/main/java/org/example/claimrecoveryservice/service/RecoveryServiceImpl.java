package org.example.claimrecoveryservice.service;

import lombok.RequiredArgsConstructor;

import org.example.claimrecoveryservice.dto.CreateRecoveryRequest;
import org.example.claimrecoveryservice.dto.RecoveryResponse;
import org.example.claimrecoveryservice.dto.UpdateRecoveryStatusRequest;
import org.example.claimrecoveryservice.entity.Recovery;
import org.example.claimrecoveryservice.exception.ClaimAccessDeniedException;
import org.example.claimrecoveryservice.exception.ClaimNotFoundException;
import org.example.claimrecoveryservice.exception.RecoveryNotFoundException;
import org.example.claimrecoveryservice.model.RecoveryStatus;
import org.example.claimrecoveryservice.repository.ClaimRepository;
import org.example.claimrecoveryservice.repository.RecoveryRepository;
import org.example.claimrecoveryservice.integration.NotificationAuditPublisher;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecoveryServiceImpl implements RecoveryService {

    private final RecoveryRepository recoveryRepository;
    private final ClaimRepository claimRepository;
    private final NotificationAuditPublisher eventPublisher;

    // =====================================================
    // CREATE RECOVERY
    // Only CLAIMS_ADJUSTER and ADMIN
    // =====================================================

    @Override
    public Mono<RecoveryResponse> createRecovery(
            Long claimId,
            CreateRecoveryRequest request,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        if (!canManageRecovery(normalizedRole)) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Only CLAIMS_ADJUSTER or ADMIN can create recovery records"
                    )
            );
        }

        if (claimId == null || claimId <= 0) {
            return Mono.error(
                    new IllegalArgumentException(
                            "A valid claim ID is required"
                    )
            );
        }

        return claimRepository.findById(claimId)
                .switchIfEmpty(
                        Mono.error(
                                new ClaimNotFoundException(claimId)
                        )
                )
                .flatMap(claim -> {

                    LocalDateTime now = LocalDateTime.now();

                    Recovery recovery = Recovery.builder()
                            .claimId(claimId)
                            .ownerId(claim.getOwnerId())
                            .recoveryAmount(request.recoveryAmount())
                            .recoverySource(request.recoverySource())
                            .description(request.description())
                            .status(RecoveryStatus.INITIATED)
                            .processedBy(userId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return recoveryRepository.save(recovery)
                            .map(this::toResponse)
                            .flatMap(response -> eventPublisher.publish(
                                            userId,
                                            normalizedRole,
                                            "RECOVERY_CREATED",
                                            "RECOVERY",
                                            response.id(),
                                            response.ownerId(),
                                            "Recovery record created",
                                            "A recovery record has been created for your claim "
                                                    + claimId + ".",
                                            "Recovery record " + response.id()
                                                    + " was created for claim "
                                                    + claimId + "."
                                    )
                                    .thenReturn(response));
                });
    }

    // =====================================================
    // GET RECOVERIES FOR A CLAIM
    // Owner: own claim only
    // Staff: all claims
    // =====================================================

    @Override
    public Flux<RecoveryResponse> getClaimRecoveries(
            Long claimId,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        if (claimId == null || claimId <= 0) {
            return Flux.error(
                    new IllegalArgumentException(
                            "A valid claim ID is required"
                    )
            );
        }

        return claimRepository.findById(claimId)
                .switchIfEmpty(
                        Mono.error(
                                new ClaimNotFoundException(claimId)
                        )
                )
                .flatMapMany(claim -> {

                    if (isStaff(normalizedRole)) {
                        return recoveryRepository
                                .findByClaimId(claimId)
                                .map(this::toResponse);
                    }

                    if ("BUSINESS_OWNER".equals(normalizedRole)
                            && userId.equals(claim.getOwnerId())) {

                        return recoveryRepository
                                .findByClaimId(claimId)
                                .map(this::toResponse);
                    }

                    return Flux.error(
                            new ClaimAccessDeniedException(
                                    "You cannot access recovery records for this claim"
                            )
                    );
                });
    }

    // =====================================================
    // GET ALL RECOVERIES
    // Staff only
    // =====================================================

    @Override
    public Flux<RecoveryResponse> getAllRecoveries(
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        if (!isStaff(normalizedRole)) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "You do not have permission to view all recovery records"
                    )
            );
        }

        return recoveryRepository.findAll()
                .map(this::toResponse);
    }

    // =====================================================
    // UPDATE RECOVERY STATUS
    // Only CLAIMS_ADJUSTER and ADMIN
    // =====================================================

    @Override
    public Mono<RecoveryResponse> updateRecoveryStatus(
            Long recoveryId,
            UpdateRecoveryStatusRequest request,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        if (!canManageRecovery(normalizedRole)) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Only CLAIMS_ADJUSTER or ADMIN can update recovery status"
                    )
            );
        }

        if (recoveryId == null || recoveryId <= 0) {
            return Mono.error(
                    new IllegalArgumentException(
                            "A valid recovery ID is required"
                    )
            );
        }

        return recoveryRepository.findById(recoveryId)
                .switchIfEmpty(
                        Mono.error(
                                new RecoveryNotFoundException(recoveryId)
                        )
                )
                .flatMap(recovery -> {

                    RecoveryStatus previousStatus = recovery.getStatus();

                    recovery.setStatus(request.status());
                    recovery.setProcessedBy(userId);
                    recovery.setUpdatedAt(LocalDateTime.now());

                    return recoveryRepository.save(recovery)
                            .map(this::toResponse)
                            .flatMap(response -> eventPublisher.publish(
                                            userId,
                                            normalizedRole,
                                            "RECOVERY_STATUS_UPDATED",
                                            "RECOVERY",
                                            response.id(),
                                            response.ownerId(),
                                            "Recovery status updated",
                                            "The recovery status for your claim "
                                                    + recovery.getClaimId()
                                                    + " has been updated from "
                                                    + previousStatus
                                                    + " to "
                                                    + response.status()
                                                    + ".",
                                            "Recovery record "
                                                    + response.id()
                                                    + " for claim "
                                                    + recovery.getClaimId()
                                                    + " changed status from "
                                                    + previousStatus
                                                    + " to "
                                                    + response.status()
                                                    + "."
                                    )
                                    .thenReturn(response));
                });
    }

    // =====================================================
    // ROLE NORMALIZATION
    // =====================================================

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

    // =====================================================
    // RECOVERY MANAGEMENT PERMISSION
    // =====================================================

    private boolean canManageRecovery(String role) {

        return "ADMIN".equals(role)
                || "CLAIMS_ADJUSTER".equals(role);
    }

    // =====================================================
    // STAFF READ PERMISSION
    // =====================================================

    private boolean isStaff(String role) {

        return "ADMIN".equals(role)
                || "CLAIMS_ADJUSTER".equals(role)
                || "RISK_ENGINEER".equals(role)
                || "UNDERWRITER".equals(role);
    }

    // =====================================================
    // ENTITY TO RESPONSE
    // =====================================================

    private RecoveryResponse toResponse(Recovery recovery) {

        return new RecoveryResponse(
                recovery.getId(),
                recovery.getClaimId(),
                recovery.getOwnerId(),
                recovery.getRecoveryAmount(),
                recovery.getRecoverySource(),
                recovery.getDescription(),
                recovery.getStatus(),
                recovery.getProcessedBy(),
                recovery.getCreatedAt(),
                recovery.getUpdatedAt()
        );
    }
}