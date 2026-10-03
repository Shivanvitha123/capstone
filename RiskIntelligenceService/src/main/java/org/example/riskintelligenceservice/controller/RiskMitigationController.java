package org.example.riskintelligenceservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.riskintelligenceservice.dto.RiskMitigationRequest;
import org.example.riskintelligenceservice.dto.RiskMitigationResponse;
import org.example.riskintelligenceservice.dto.RiskMitigationStatusRequest;
import org.example.riskintelligenceservice.service.RiskMitigationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/risk/mitigations")
@RequiredArgsConstructor
public class RiskMitigationController {

    private final RiskMitigationService service;

    @PostMapping
    public Mono<ResponseEntity<RiskMitigationResponse>> createMitigation(
            @Valid @RequestBody RiskMitigationRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return service.createMitigation(request, userId, role)
                .map(response ->
                        ResponseEntity.status(HttpStatus.CREATED)
                                .body(response)
                );
    }

    @GetMapping
    public Flux<RiskMitigationResponse> getAllMitigations(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return service.getAllMitigations(userId, role);
    }

    @GetMapping("/{id}")
    public Mono<RiskMitigationResponse> getMitigation(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return service.getMitigation(id, userId, role);
    }

    @GetMapping("/assessment/{assessmentId}")
    public Flux<RiskMitigationResponse> getByAssessment(
            @PathVariable Long assessmentId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return service.getByAssessment(assessmentId, userId, role);
    }

    @PatchMapping("/{id}/status")
    public Mono<RiskMitigationResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RiskMitigationStatusRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return service.updateStatus(
                id,
                request.status(),
                userId,
                role
        );
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        try {
            return Long.valueOf(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated user ID must be numeric"
            );
        }
    }

    private String getRole(Authentication authentication) {
        if (authentication == null
                || authentication.getAuthorities() == null
                || authentication.getAuthorities().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Authenticated user role is missing"
            );
        }

        return authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replaceFirst("^ROLE_", "")
                .trim()
                .toUpperCase();
    }
}