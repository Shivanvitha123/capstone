package org.example.notificationauditservice.controller;

import org.example.notificationauditservice.dto.AdminDashboardResponse;
import org.example.notificationauditservice.service.AdminAnalyticsService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(
            AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public Mono<ResponseEntity<AdminDashboardResponse>> getDashboard(
            Authentication authentication,
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            ) String authorization) {

        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authentication required"
                    )
            );
        }

        // Check whether the authenticated user is an administrator
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority().trim())
                .map(String::toUpperCase)
                .map(authority ->
                        authority.startsWith("ROLE_")
                                ? authority.substring(5)
                                : authority
                )
                .anyMatch("ADMIN"::equals);

        if (!isAdmin) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Only administrators can access this dashboard"
                    )
            );
        }

        // The downstream analytics service needs the access token
        if (authorization == null || authorization.isBlank()) {
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED,
                            "Authorization header is missing"
                    )
            );
        }

        return analyticsService.getDashboard(authorization)
                .map(ResponseEntity::ok);
    }
}