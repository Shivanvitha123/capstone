package org.example.claimrecoveryservice.dto;

import java.math.BigDecimal;

public record ClaimAnalyticsResponse(
        long totalClaims,
        long submittedClaims,
        long underReviewClaims,
        long approvedClaims,
        long rejectedClaims,
        long settledClaims,
        BigDecimal totalClaimedAmount,
        BigDecimal totalApprovedAmount,
        BigDecimal totalSettledAmount
) {
}