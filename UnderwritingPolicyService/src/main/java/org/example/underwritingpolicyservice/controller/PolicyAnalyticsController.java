package org.example.underwritingpolicyservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.PolicyAnalyticsResponse;
import org.example.underwritingpolicyservice.service.PolicyAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/policies/admin/analytics")
@RequiredArgsConstructor
public class PolicyAnalyticsController {

    private final PolicyAnalyticsService analyticsService;

    @GetMapping
    public Mono<ResponseEntity<PolicyAnalyticsResponse>> getAnalytics(
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return Mono.error(
                    new AccessDeniedException(
                            "Only ADMIN can access policy analytics"
                    )
            );
        }

        return analyticsService.getPolicyAnalytics()
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