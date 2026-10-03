package org.example.underwritingpolicyservice.dto;

import java.math.BigDecimal;

public record PolicyAnalyticsResponse(
        long totalPolicies,
        long activePolicies,
        long draftPolicies,
        long submittedPolicies,
        long monthlyPaymentPolicies,
        long yearlyPaymentPolicies,
        BigDecimal totalPremiumValue,
        BigDecimal totalCoverage
) {
}