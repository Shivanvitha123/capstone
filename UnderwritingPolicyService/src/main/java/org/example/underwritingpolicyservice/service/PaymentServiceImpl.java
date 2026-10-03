package org.example.underwritingpolicyservice.service;

import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.PaymentResponse;
import org.example.underwritingpolicyservice.entity.Payment;
import org.example.underwritingpolicyservice.entity.Policy;
import org.example.underwritingpolicyservice.exception.PolicyAccessDeniedException;
import org.example.underwritingpolicyservice.exception.PolicyNotFoundException;
import org.example.underwritingpolicyservice.model.PolicyStatus;
import org.example.underwritingpolicyservice.repository.PaymentRepository;
import org.example.underwritingpolicyservice.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final PolicyEventPublisher eventPublisher;

    @Override
    public Mono<PaymentResponse> makePayment(
            Long policyId,
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only business owners can make payments"
                    )
            );
        }

        if (userId == null) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        return policyRepository.findById(policyId)
                .switchIfEmpty(Mono.error(
                        new PolicyNotFoundException(
                                "Policy not found: " + policyId
                        )
                ))
                .flatMap(policy -> {
                    if (!userId.equals(policy.getOwnerId())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "You cannot pay for another owner's policy"
                                )
                        );
                    }

                    if (!PolicyStatus.APPROVED.name()
                            .equals(policy.getStatus())
                            && !PolicyStatus.ACTIVE.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Only APPROVED or ACTIVE policies can be paid"
                                )
                        );
                    }

                    if (policy.getPremiumAmount() == null
                            || policy.getPremiumAmount().signum() <= 0) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Policy premium must be greater than zero"
                                )
                        );
                    }

                    LocalDateTime now = LocalDateTime.now();

                    Payment payment = Payment.builder()
                            .policyId(policy.getId())
                            .ownerId(userId)
                            .amount(policy.getPremiumAmount())
                            .paymentDate(now)
                            .status("SUCCESS")
                            .transactionReference(
                                    "MOCK-" + UUID.randomUUID()
                            )
                            .createdAt(now)
                            .build();

                    return paymentRepository.save(payment)
                            .flatMap(savedPayment -> {
                                PaymentResponse response =
                                        toResponse(savedPayment);

                                return eventPublisher.publish(
                                        userId,
                                        role,
                                        "POLICY_PAYMENT_SUCCESS",
                                        "PAYMENT",
                                        savedPayment.getId(),
                                        "Payment of "
                                                + savedPayment.getAmount()
                                                + " was successful for policy "
                                                + policy.getPolicyNumber()
                                                + ". Transaction reference: "
                                                + savedPayment.getTransactionReference(),
                                        policy.getOwnerId(),
                                        "PAYMENT",
                                        "Payment Successful",
                                        "Your payment of "
                                                + savedPayment.getAmount()
                                                + " for policy "
                                                + policy.getPolicyNumber()
                                                + " was successful."
                                ).thenReturn(response);
                            });
                });
    }

    @Override
    public Flux<PaymentResponse> getPolicyPayments(
            Long policyId,
            Long userId,
            String role) {

        if (userId == null) {
            return Flux.error(
                    new PolicyAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        return policyRepository.findById(policyId)
                .switchIfEmpty(Mono.error(
                        new PolicyNotFoundException(
                                "Policy not found: " + policyId
                        )
                ))
                .flatMapMany(policy -> {
                    if (isRole(role, "ADMIN")
                            || userId.equals(policy.getOwnerId())
                            && isRole(role, "BUSINESS_OWNER")) {

                        return paymentRepository
                                .findByPolicyId(policyId)
                                .map(this::toResponse);
                    }

                    return Flux.error(
                            new PolicyAccessDeniedException(
                                    "You cannot view these payments"
                            )
                    );
                });
    }

    @Override
    public Flux<PaymentResponse> getAllPayments(String role) {
        if (!isRole(role, "ADMIN")) {
            return Flux.error(
                    new PolicyAccessDeniedException(
                            "Only administrators can view all payments"
                    )
            );
        }

        return paymentRepository.findAll()
                .map(this::toResponse);
    }

    private boolean isRole(
            String actualRole,
            String expectedRole) {

        return actualRole != null
                && expectedRole.equalsIgnoreCase(
                actualRole.replaceFirst("^ROLE_", "")
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPolicyId(),
                payment.getOwnerId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getStatus(),
                payment.getTransactionReference(),
                payment.getCreatedAt()
        );
    }
}