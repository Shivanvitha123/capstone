package org.example.underwritingpolicyservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.PaymentResponse;
import org.example.underwritingpolicyservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/policies/{policyId}")
    public Mono<ResponseEntity<PaymentResponse>> makePayment(
            @PathVariable Long policyId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return paymentService
                .makePayment(policyId, userId, role)
                .map(response ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response)
                );
    }

    @GetMapping("/policies/{policyId}")
    public Flux<PaymentResponse> getPolicyPayments(
            @PathVariable Long policyId,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        String role = getRole(authentication);

        return paymentService.getPolicyPayments(
                policyId,
                userId,
                role
        );
    }

    @GetMapping
    public Flux<PaymentResponse> getAllPayments(
            Authentication authentication) {

        String role = getRole(authentication);

        return paymentService.getAllPayments(role);
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
        } catch (NumberFormatException exception) {
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