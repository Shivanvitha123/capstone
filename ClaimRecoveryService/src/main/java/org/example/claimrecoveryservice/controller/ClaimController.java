package org.example.claimrecoveryservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.example.claimrecoveryservice.dto.ClaimResponse;
import org.example.claimrecoveryservice.dto.CreateClaimRequest;
import org.example.claimrecoveryservice.dto.UpdateClaimStatusRequest;
import org.example.claimrecoveryservice.service.ClaimEventPublisher;
import org.example.claimrecoveryservice.service.ClaimFileService;
import org.example.claimrecoveryservice.service.ClaimService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService service;
    private final ClaimFileService claimFileService;
    private final ClaimEventPublisher eventPublisher;

    private static final Set<String> VALID_ROLES = Set.of(
            "ADMIN",
            "BUSINESS_OWNER",
            "UNDERWRITER",
            "RISK_ENGINEER",
            "CLAIMS_ADJUSTER"
    );

    // =====================================================
    // CREATE CLAIM
    // =====================================================

    @PostMapping
    public Mono<ResponseEntity<ClaimResponse>> createClaim(
            @Valid @RequestBody CreateClaimRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.createClaim(request, userId, role)
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "CLAIM_CREATED",
                                "CLAIM",
                                response.id(),
                                "Claim " + response.claimNumber()
                                        + " was created.",
                                response.ownerId(),
                                "SUCCESS",
                                "Claim Created",
                                "Your claim " + response.claimNumber()
                                        + " was successfully created."
                        ).thenReturn(
                                ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(response)
                        )
                );
    }

    // =====================================================
    // GET CLAIMS
    // BUSINESS OWNER: OWN CLAIMS
    // STAFF: ALL CLAIMS
    // =====================================================

    @GetMapping
    public Mono<ResponseEntity<Flux<ClaimResponse>>> getAllClaims(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        Flux<ClaimResponse> claims;

        if ("BUSINESS_OWNER".equals(role)) {
            claims = service.getOwnerClaims(userId, role);
        } else {
            claims = service.getAllClaims(userId, role);
        }

        return Mono.just(ResponseEntity.ok(claims));
    }

    // =====================================================
    // GET OWNER CLAIMS
    // =====================================================

    @GetMapping("/owner")
    public Mono<ResponseEntity<Flux<ClaimResponse>>> getOwnerClaims(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return Mono.just(
                ResponseEntity.ok(
                        service.getOwnerClaims(userId, role)
                )
        );
    }

    // =====================================================
    // GET CLAIM BY ID
    // =====================================================

    @GetMapping("/{claimId}")
    public Mono<ClaimResponse> getClaim(
            @PathVariable Long claimId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getClaim(claimId, userId, role);
    }

    // =====================================================
    // UPDATE CLAIM STATUS
    // =====================================================

    @PatchMapping("/{claimId}/status")
    public Mono<ClaimResponse> updateClaimStatus(
            @PathVariable Long claimId,
            @Valid @RequestBody UpdateClaimStatusRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.updateClaimStatus(
                        claimId,
                        request,
                        userId,
                        role
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "CLAIM_STATUS_UPDATED",
                                "CLAIM",
                                response.id(),
                                "Claim " + response.claimNumber()
                                        + " status changed to "
                                        + response.status() + ".",
                                response.ownerId(),
                                "INFO",
                                "Claim Status Updated",
                                "The status of your claim "
                                        + response.claimNumber()
                                        + " is now "
                                        + response.status() + "."
                        ).thenReturn(response)
                );
    }

    // =====================================================
    // UPLOAD CLAIM DOCUMENT
    // =====================================================

    @PostMapping(
            value = "/{claimId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Mono<ResponseEntity<String>> uploadClaimDocument(
            @PathVariable Long claimId,
            @RequestPart("file") FilePart file,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getClaim(claimId, userId, role)
                .flatMap(claim ->
                        claimFileService.uploadFile(
                                claimId.toString(),
                                file
                        )
                )
                .map(filename ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(filename)
                );
    }

    // =====================================================
    // GET CLAIM DOCUMENTS
    // =====================================================

    @GetMapping("/{claimId}/documents")
    public Mono<ResponseEntity<List<String>>> getClaimDocuments(
            @PathVariable Long claimId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getClaim(claimId, userId, role)
                .then(
                        claimFileService.listFiles(
                                claimId.toString()
                        )
                )
                .map(ResponseEntity::ok);
    }

    // =====================================================
    // DOWNLOAD CLAIM DOCUMENT
    // =====================================================

    @GetMapping("/{claimId}/documents/{filename}")
    public Mono<ResponseEntity<Resource>> downloadClaimDocument(
            @PathVariable Long claimId,
            @PathVariable String filename,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        if (userId == null || role == null) {
            return Mono.error(
                    new AccessDeniedException(
                            "Valid user authentication is required"
                    )
            );
        }

        return service.getClaim(claimId, userId, role)
                .then(
                        claimFileService.getFile(
                                claimId.toString(),
                                filename
                        )
                )
                .map(path -> {
                    Resource resource = new FileSystemResource(path);

                    return ResponseEntity.ok()
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\""
                                            + filename + "\""
                            )
                            .contentType(
                                    MediaType.APPLICATION_OCTET_STREAM
                            )
                            .body(resource);
                });
    }

    // =====================================================
    // AUTHENTICATION HELPERS
    // =====================================================

    private Long getUserId(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
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
                .map(authority ->
                        normalizeRole(authority.getAuthority())
                )
                .filter(VALID_ROLES::contains)
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