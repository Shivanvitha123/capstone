package org.example.underwritingpolicyservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.CreatePolicyRequest;
import org.example.underwritingpolicyservice.dto.PolicyResponse;
import org.example.underwritingpolicyservice.dto.UpdatePolicyRequest;
import org.example.underwritingpolicyservice.model.PolicyStatus;
import org.example.underwritingpolicyservice.service.PolicyEventPublisher;
import org.example.underwritingpolicyservice.service.PolicyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;
    private final PolicyEventPublisher eventPublisher;

    @PostMapping
    public Mono<ResponseEntity<PolicyResponse>> createPolicy(
            @Valid @RequestBody CreatePolicyRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .createPolicy(request, userId, role)
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "POLICY_CREATED",
                                "POLICY",
                                response.id(),
                                "Policy " + response.policyNumber()
                                        + " was created.",
                                response.ownerId(),
                                "SUCCESS",
                                "Policy Created",
                                "Your policy "
                                        + response.policyNumber()
                                        + " was created successfully."
                        ).thenReturn(
                                ResponseEntity
                                        .status(HttpStatus.CREATED)
                                        .body(response)
                        )
                );
    }

    @GetMapping("/owner")
    public Flux<PolicyResponse> getOwnerPolicies(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService.getOwnerPolicies(userId, role);
    }

    @GetMapping
    public Flux<PolicyResponse> getAllPolicies(
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService.getAllPolicies(userId, role);
    }

    @GetMapping("/{policyId}")
    public Mono<ResponseEntity<PolicyResponse>> getPolicy(
            @PathVariable Long policyId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .getPolicy(policyId, userId, role)
                .map(ResponseEntity::ok);
    }

    @PutMapping("/{policyId}")
    public Mono<ResponseEntity<PolicyResponse>> updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody UpdatePolicyRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .updatePolicy(policyId, request, userId, role)
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "POLICY_UPDATED",
                                "POLICY",
                                response.id(),
                                "Policy " + response.policyNumber()
                                        + " was updated.",
                                response.ownerId(),
                                "INFO",
                                "Policy Updated",
                                "Your policy "
                                        + response.policyNumber()
                                        + " was updated."
                        ).thenReturn(ResponseEntity.ok(response))
                );
    }

    @PostMapping("/{policyId}/submit")
    public Mono<ResponseEntity<PolicyResponse>> submitPolicy(
            @PathVariable Long policyId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .submitPolicy(policyId, userId, role)
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "POLICY_SUBMITTED",
                                "POLICY",
                                response.id(),
                                "Policy " + response.policyNumber()
                                        + " was submitted for review.",
                                response.ownerId(),
                                "INFO",
                                "Policy Submitted",
                                "Your policy "
                                        + response.policyNumber()
                                        + " was submitted for review."
                        ).thenReturn(ResponseEntity.ok(response))
                );
    }

    @PatchMapping("/{policyId}/status")
    public Mono<ResponseEntity<PolicyResponse>> updatePolicyStatus(
            @PathVariable Long policyId,
            @RequestParam PolicyStatus status,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .updatePolicyStatus(policyId, status, userId, role)
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "POLICY_STATUS_UPDATED",
                                "POLICY",
                                response.id(),
                                "Policy " + response.policyNumber()
                                        + " status changed to "
                                        + response.status() + ".",
                                response.ownerId(),
                                "INFO",
                                "Policy Status Updated",
                                "The status of policy "
                                        + response.policyNumber()
                                        + " is now "
                                        + response.status() + "."
                        ).thenReturn(ResponseEntity.ok(response))
                );
    }

    @PostMapping("/{policyId}/cancellation")
    public Mono<ResponseEntity<PolicyResponse>> requestCancellation(
            @PathVariable Long policyId,
            @RequestBody CancellationRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .requestCancellation(
                        policyId,
                        request.reason(),
                        userId,
                        role
                )
                .flatMap(response ->
                        eventPublisher.publish(
                                userId,
                                role,
                                "POLICY_CANCELLATION_REQUESTED",
                                "POLICY",
                                response.id(),
                                "Cancellation requested for policy "
                                        + response.policyNumber()
                                        + ". Reason: "
                                        + request.reason(),
                                response.ownerId(),
                                "ACTION_REQUIRED",
                                "Cancellation Requested",
                                "A cancellation request was submitted for policy "
                                        + response.policyNumber() + "."
                        ).thenReturn(ResponseEntity.ok(response))
                );
    }

    @PatchMapping("/{policyId}/cancellation")
    public Mono<ResponseEntity<PolicyResponse>> reviewCancellation(
            @PathVariable Long policyId,
            @RequestBody CancellationReviewRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return policyService
                .reviewCancellation(
                        policyId,
                        request.approved(),
                        userId,
                        role
                )
                .flatMap(response -> {
                    String decision = request.approved()
                            ? "approved"
                            : "rejected";

                    return eventPublisher.publish(
                            userId,
                            role,
                            "POLICY_CANCELLATION_REVIEWED",
                            "POLICY",
                            response.id(),
                            "Cancellation request for policy "
                                    + response.policyNumber()
                                    + " was " + decision + ".",
                            response.ownerId(),
                            request.approved()
                                    ? "SUCCESS"
                                    : "INFO",
                            "Cancellation Request Reviewed",
                            "The cancellation request for policy "
                                    + response.policyNumber()
                                    + " was " + decision + "."
                    ).thenReturn(ResponseEntity.ok(response));
                });
    }

    public record CancellationRequest(String reason) {
    }

    public record CancellationReviewRequest(boolean approved) {
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || authentication.getPrincipal() == null) {
            return null;
        }

        try {
            return Long.parseLong(
                    authentication.getPrincipal().toString()
            );
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String getRole(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        return authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(authority ->
                        authority.getAuthority().replace("ROLE_", "")
                )
                .orElse(null);
    }
}