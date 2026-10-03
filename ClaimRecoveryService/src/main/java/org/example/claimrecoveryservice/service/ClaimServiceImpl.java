package org.example.claimrecoveryservice.service;

import lombok.RequiredArgsConstructor;

import org.example.claimrecoveryservice.dto.ClaimResponse;
import org.example.claimrecoveryservice.dto.CreateClaimRequest;
import org.example.claimrecoveryservice.dto.UpdateClaimStatusRequest;
import org.example.claimrecoveryservice.entity.Claim;
import org.example.claimrecoveryservice.exception.ClaimAccessDeniedException;
import org.example.claimrecoveryservice.exception.ClaimNotFoundException;
import org.example.claimrecoveryservice.model.ClaimStatus;
import org.example.claimrecoveryservice.repository.ClaimRepository;

import org.springframework.stereotype.Service;
import org.example.claimrecoveryservice.integration.NotificationAuditPublisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final ClaimRepository repository;
    private final NotificationAuditPublisher eventPublisher;

    // =====================================================
    // CREATE CLAIM
    // =====================================================

    @Override
    public Mono<ClaimResponse> createClaim(
            CreateClaimRequest request,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "User ID is required to create a claim"
                    )
            );
        }

        if (!"BUSINESS_OWNER".equals(normalizedRole)
                && !"ADMIN".equals(normalizedRole)) {

            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Only BUSINESS_OWNER or ADMIN can create a claim"
                    )
            );
        }

        return repository.findByClaimNumber(request.claimNumber())
                .flatMap(existing ->
                        Mono.<ClaimResponse>error(
                                new ClaimAccessDeniedException(
                                        "Claim number already exists"
                                )
                        )
                )
                .switchIfEmpty(
                        Mono.defer(() -> {
                            LocalDateTime now = LocalDateTime.now();

                            Claim claim = Claim.builder()
                                    .businessId(request.businessId())
                                    .policyId(request.policyId())
                                    .ownerId(userId)
                                    .claimNumber(request.claimNumber())
                                    .claimType(request.claimType())
                                    .incidentDate(request.incidentDate())
                                    .reportedDate(request.reportedDate())
                                    .claimedAmount(request.claimedAmount())
                                    .description(request.description())
                                    .status(ClaimStatus.SUBMITTED)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build();

                            return repository.save(claim)
                                    .map(this::toResponse)
                                    .flatMap(response -> eventPublisher.publish(
                                                    userId, normalizedRole, "CLAIM_CREATED",
                                                    "CLAIM", response.id(), response.ownerId(),
                                                    "Claim submitted",
                                                    "Your claim " + response.claimNumber() + " has been submitted.",
                                                    "Claim " + response.claimNumber() + " was created.")
                                            .thenReturn(response));
                        })
                );
    }

    // =====================================================
    // GET OWNER CLAIMS
    // =====================================================

    @Override
    public Flux<ClaimResponse> getOwnerClaims(
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "User ID is required to access owner claims"
                    )
            );
        }

        if (!"BUSINESS_OWNER".equals(normalizedRole)) {
            return Flux.error(
                    new ClaimAccessDeniedException(
                            "Only BUSINESS_OWNER can access owner claims"
                    )
            );
        }

        return repository.findByOwnerId(userId)
                .map(this::toResponse);
    }

    // =====================================================
    // GET ALL CLAIMS
    // =====================================================

    @Override
    public Flux<ClaimResponse> getAllClaims(
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        // Business owners can view only their own claims.
        if ("BUSINESS_OWNER".equals(normalizedRole)) {

            if (userId == null) {
                return Flux.error(
                        new ClaimAccessDeniedException(
                                "User ID is required to access claims"
                        )
                );
            }

            return repository.findByOwnerId(userId)
                    .map(this::toResponse);
        }

        // Authorized staff can view all claims.
        if (isStaff(normalizedRole)) {
            return repository.findAll()
                    .map(this::toResponse);
        }

        return Flux.error(
                new ClaimAccessDeniedException(
                        "You do not have permission to view claims"
                )
        );
    }

    // =====================================================
    // GET CLAIM BY ID
    // =====================================================

    @Override
    public Mono<ClaimResponse> getClaim(
            Long claimId,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "User ID is required to access this claim"
                    )
            );
        }

        return repository.findById(claimId)
                .switchIfEmpty(
                        Mono.error(
                                new ClaimNotFoundException(claimId)
                        )
                )
                .flatMap(claim -> {

                    // Staff can view all claims.
                    if (isStaff(normalizedRole)) {
                        return Mono.just(toResponse(claim));
                    }

                    // Owners can view only their own claims.
                    if ("BUSINESS_OWNER".equals(normalizedRole)
                            && userId.equals(claim.getOwnerId())) {
                        return Mono.just(toResponse(claim));
                    }

                    return Mono.error(
                            new ClaimAccessDeniedException(
                                    "You cannot access this claim"
                            )
                    );
                });
    }

    // =====================================================
    // UPDATE CLAIM STATUS
    // =====================================================

    @Override
    public Mono<ClaimResponse> updateClaimStatus(
            Long claimId,
            UpdateClaimStatusRequest request,
            Long userId,
            String role) {

        String normalizedRole = normalizeRole(role);

        if (userId == null) {
            return Mono.error(
                    new ClaimAccessDeniedException(
                            "User ID is required to update a claim"
                    )
            );
        }

        // Only claims adjusters and admins can update claim status.
        if (!"CLAIMS_ADJUSTER".equals(normalizedRole)
                && !"ADMIN".equals(normalizedRole)) {

            return Mono.error(
                    new ClaimAccessDeniedException(
                            "Only CLAIMS_ADJUSTER or ADMIN can change claim status"
                    )
            );
        }

        return repository.findById(claimId)
                .switchIfEmpty(
                        Mono.error(
                                new ClaimNotFoundException(claimId)
                        )
                )
                .flatMap(claim -> {

                    ClaimStatus previousStatus = claim.getStatus();
                    claim.setStatus(request.status());

                    if (request.approvedAmount() != null) {
                        claim.setApprovedAmount(
                                request.approvedAmount()
                        );
                    }

                    if (request.assignedAdjusterId() != null) {
                        claim.setAssignedAdjusterId(
                                request.assignedAdjusterId()
                        );
                    }

                    claim.setUpdatedAt(LocalDateTime.now());

                    return repository.save(claim)
                            .map(this::toResponse)
                            .flatMap(response -> eventPublisher.publish(
                                            userId, normalizedRole, "CLAIM_STATUS_UPDATED",
                                            "CLAIM", response.id(), response.ownerId(),
                                            "Claim status updated",
                                            "Your claim " + response.claimNumber() + " status changed to " + response.status() + ".",
                                            "Claim " + response.claimNumber() + " status changed from " + previousStatus + " to " + response.status() + ".")
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
    // STAFF ROLE CHECK
    // =====================================================

    private boolean isStaff(String role) {

        String normalizedRole = normalizeRole(role);

        return "ADMIN".equals(normalizedRole)
                || "CLAIMS_ADJUSTER".equals(normalizedRole)
                || "RISK_ENGINEER".equals(normalizedRole)
                || "UNDERWRITER".equals(normalizedRole);
    }

    // =====================================================
    // ENTITY TO RESPONSE
    // =====================================================

    private ClaimResponse toResponse(Claim claim) {

        return new ClaimResponse(
                claim.getId(),
                claim.getBusinessId(),
                claim.getPolicyId(),
                claim.getOwnerId(),
                claim.getClaimNumber(),
                claim.getClaimType(),
                claim.getIncidentDate(),
                claim.getReportedDate(),
                claim.getClaimedAmount(),
                claim.getApprovedAmount(),
                claim.getDescription(),
                claim.getStatus(),
                claim.getAssignedAdjusterId(),
                claim.getCreatedAt(),
                claim.getUpdatedAt()
        );
    }
}