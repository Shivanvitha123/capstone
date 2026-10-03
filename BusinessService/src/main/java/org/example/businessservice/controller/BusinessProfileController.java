package org.example.businessservice.controller;

import jakarta.validation.Valid;

import org.example.businessservice.dto.BusinessDeletionRequestResponse;
import org.example.businessservice.dto.BusinessProfileResponse;
import org.example.businessservice.dto.BusinessSummaryResponse;
import org.example.businessservice.dto.CreateBusinessProfileRequest;
import org.example.businessservice.dto.UpdateBusinessProfileRequest;
import org.example.businessservice.service.BusinessEventPublisher;
import org.example.businessservice.service.BusinessProfileService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/business")
public class BusinessProfileController {

    private final BusinessProfileService businessProfileService;
    private final BusinessEventPublisher eventPublisher;

    public BusinessProfileController(
            BusinessProfileService businessProfileService,
            BusinessEventPublisher eventPublisher
    ) {
        this.businessProfileService = businessProfileService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BusinessProfileResponse> createBusinessProfile(
            @Valid @RequestBody CreateBusinessProfileRequest request,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return businessProfileService.createBusinessProfile(
                        userId,
                        request
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                        new BusinessEventPublisher.BusinessEventRequest(
                                                userId,
                                                getRole(authentication),
                                                "BUSINESS_CREATED",
                                                "BUSINESS",
                                                response.id(),
                                                "Business profile created: "
                                                        + response.businessName(),
                                                userId,
                                                "SUCCESS",
                                                "Business profile created",
                                                "Your business profile "
                                                        + response.businessName()
                                                        + " has been created successfully."
                                        ),
                                        getAuthorization(exchange)
                                )
                                .thenReturn(response)
                );
    }

    @GetMapping("/me")
    public Flux<BusinessProfileResponse> getMyBusinessProfiles(
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return businessProfileService.getMyBusinessProfiles(userId);
    }

    @GetMapping("/{businessId}")
    public Mono<BusinessProfileResponse> getBusinessProfile(
            @PathVariable Long businessId,
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        String role = getRole(authentication);

        return businessProfileService.getBusinessProfile(
                businessId,
                userId,
                role
        );
    }

    @PutMapping("/{businessId}")
    public Mono<BusinessProfileResponse> updateBusinessProfile(
            @PathVariable Long businessId,
            @Valid @RequestBody UpdateBusinessProfileRequest request,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        Long userId = Long.valueOf(authentication.getName());

        return businessProfileService.updateBusinessProfile(
                        businessId,
                        userId,
                        request
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                        new BusinessEventPublisher.BusinessEventRequest(
                                                userId,
                                                getRole(authentication),
                                                "BUSINESS_UPDATED",
                                                "BUSINESS",
                                                response.id(),
                                                "Business profile updated: "
                                                        + response.businessName(),
                                                userId,
                                                "INFO",
                                                "Business profile updated",
                                                "Your business profile "
                                                        + response.businessName()
                                                        + " has been updated."
                                        ),
                                        getAuthorization(exchange)
                                )
                                .thenReturn(response)
                );
    }

    @GetMapping
    public Flux<BusinessSummaryResponse> getAllBusinessProfiles(
            Authentication authentication
    ) {
        return businessProfileService.getAllBusinessProfiles();
    }

    // Business deletion request endpoints

    @PostMapping("/{businessId}/deletion-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<BusinessDeletionRequestResponse> requestDeletion(
            @PathVariable Long businessId,
            @RequestBody(required = false) DeletionReason request,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        Long ownerId = Long.valueOf(authentication.getName());

        String reason = request == null ? "" : request.reason();

        return businessProfileService.requestDeletion(
                        businessId,
                        ownerId,
                        reason
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                        new BusinessEventPublisher.BusinessEventRequest(
                                                ownerId,
                                                getRole(authentication),
                                                "BUSINESS_DELETION_REQUESTED",
                                                "BUSINESS",
                                                businessId,
                                                "Business deletion requested. Request ID: "
                                                        + response.id(),
                                                ownerId,
                                                "ACTION_REQUIRED",
                                                "Business deletion request submitted",
                                                "Your deletion request for business "
                                                        + businessId
                                                        + " has been submitted for review."
                                        ),
                                        getAuthorization(exchange)
                                )
                                .thenReturn(response)
                );
    }

    @GetMapping("/deletion-requests/me")
    public Flux<BusinessDeletionRequestResponse> getMyDeletionRequests(
            Authentication authentication
    ) {
        Long ownerId = Long.valueOf(authentication.getName());

        return businessProfileService.getMyDeletionRequests(ownerId);
    }

    @GetMapping("/deletion-requests/pending")
    public Flux<BusinessDeletionRequestResponse> getPendingDeletionRequests(
            Authentication authentication
    ) {
        return businessProfileService.getPendingDeletionRequests(
                getRole(authentication)
        );
    }

    @PutMapping("/deletion-requests/{requestId}/approve")
    public Mono<BusinessDeletionRequestResponse> approveDeletionRequest(
            @PathVariable Long requestId,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        Long reviewerId = Long.valueOf(authentication.getName());

        return businessProfileService.reviewDeletionRequest(
                        requestId,
                        reviewerId,
                        getRole(authentication),
                        true
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                        new BusinessEventPublisher.BusinessEventRequest(
                                                reviewerId,
                                                getRole(authentication),
                                                "BUSINESS_DELETION_APPROVED",
                                                "BUSINESS",
                                                response.businessId(),
                                                "Business deletion request approved. Request ID: "
                                                        + response.id(),
                                                response.ownerId(),
                                                "WARNING",
                                                "Business deletion approved",
                                                "Your deletion request for business "
                                                        + response.businessId()
                                                        + " has been approved."
                                        ),
                                        getAuthorization(exchange)
                                )
                                .thenReturn(response)
                );
    }

    @PutMapping("/deletion-requests/{requestId}/reject")
    public Mono<BusinessDeletionRequestResponse> rejectDeletionRequest(
            @PathVariable Long requestId,
            Authentication authentication,
            ServerWebExchange exchange
    ) {
        Long reviewerId = Long.valueOf(authentication.getName());

        return businessProfileService.reviewDeletionRequest(
                        requestId,
                        reviewerId,
                        getRole(authentication),
                        false
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                        new BusinessEventPublisher.BusinessEventRequest(
                                                reviewerId,
                                                getRole(authentication),
                                                "BUSINESS_DELETION_REJECTED",
                                                "BUSINESS",
                                                response.businessId(),
                                                "Business deletion request rejected. Request ID: "
                                                        + response.id(),
                                                response.ownerId(),
                                                "INFO",
                                                "Business deletion rejected",
                                                "Your deletion request for business "
                                                        + response.businessId()
                                                        + " has been rejected."
                                        ),
                                        getAuthorization(exchange)
                                )
                                .thenReturn(response)
                );
    }

    private String getRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority().replace("ROLE_", "")
                )
                .orElse("");
    }

    private String getAuthorization(ServerWebExchange exchange) {
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }

        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst("ACCESS_TOKEN");

        return cookie == null ? null : cookie.getValue();
    }

    public record DeletionReason(String reason) {
    }
}