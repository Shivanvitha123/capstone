package org.example.notificationauditservice.dto;

import java.math.BigDecimal;

public record BusinessAnalyticsResponse(
        Long businessId,
        long totalPolicies,
        long activePolicies,
        BigDecimal totalPremium,
        long totalClaims,
        BigDecimal totalClaimsPaid
) {}