package org.example.claimrecoveryservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.claimrecoveryservice.dto.ClaimAnalyticsResponse;
import org.example.claimrecoveryservice.service.ClaimAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/claims/admin/analytics")
@RequiredArgsConstructor
public class ClaimAnalyticsController {

    private final ClaimAnalyticsService analyticsService;

    @GetMapping
    public Mono<ResponseEntity<ClaimAnalyticsResponse>> getAnalytics(
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return Mono.error(
                    new AccessDeniedException(
                            "Only ADMIN can access claim analytics"
                    )
            );
        }

        return analyticsService.getClaimAnalytics()
                .map(ResponseEntity::ok);
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority()
                            .replaceFirst("^ROLE_", "");

                    return "ADMIN".equalsIgnoreCase(role);
                });
    }
}