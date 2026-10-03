package org.example.underwritingpolicyservice.dto;

import java.math.BigDecimal;

public record StandardPolicy(
        String id,
        String name,
        String policyType,
        String description,
        BigDecimal suggestedCoverageAmount,
        BigDecimal suggestedPremiumAmount,
        String coverageDetails,
        String recommendedBusinessType
) {
}