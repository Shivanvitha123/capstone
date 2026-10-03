package org.example.underwritingpolicyservice.service;

import org.example.underwritingpolicyservice.dto.PaymentResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaymentService {

    Mono<PaymentResponse> makePayment(
            Long policyId,
            Long userId,
            String role
    );

    Flux<PaymentResponse> getPolicyPayments(
            Long policyId,
            Long userId,
            String role
    );

    Flux<PaymentResponse> getAllPayments(
            String role
    );
}