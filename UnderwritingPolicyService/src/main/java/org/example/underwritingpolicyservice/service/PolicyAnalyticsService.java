package org.example.underwritingpolicyservice.service;

import org.example.underwritingpolicyservice.dto.PolicyAnalyticsResponse;
import org.example.underwritingpolicyservice.entity.Policy;
import org.example.underwritingpolicyservice.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyAnalyticsService {

    private final PolicyRepository policyRepository;

    public Mono<PolicyAnalyticsResponse> getPolicyAnalytics() {
        return policyRepository.findAll()
                .collectList()
                .map(this::calculateAnalytics);
    }

    private PolicyAnalyticsResponse calculateAnalytics(
            List<Policy> policies) {

        long total = policies.size();

        long active = policies.stream()
                .filter(p -> hasValue(p.getStatus(), "ACTIVE"))
                .count();

        long draft = policies.stream()
                .filter(p -> hasValue(p.getStatus(), "DRAFT"))
                .count();

        long submitted = policies.stream()
                .filter(p -> hasValue(p.getStatus(), "SUBMITTED"))
                .count();

        long monthly = policies.stream()
                .filter(p -> hasValue(
                        p.getPaymentFrequency(), "MONTHLY"))
                .count();

        long yearly = policies.stream()
                .filter(p -> hasValue(
                        p.getPaymentFrequency(), "YEARLY"))
                .count();

        BigDecimal premiumTotal = policies.stream()
                .map(Policy::getPremiumAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal coverageTotal = policies.stream()
                .map(Policy::getCoverageAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PolicyAnalyticsResponse(
                total,
                active,
                draft,
                submitted,
                monthly,
                yearly,
                premiumTotal,
                coverageTotal
        );
    }

    private boolean hasValue(String actual, String expected) {
        return actual != null
                && actual.equalsIgnoreCase(expected);
    }
}