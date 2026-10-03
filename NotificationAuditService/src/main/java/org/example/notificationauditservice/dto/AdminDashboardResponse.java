package org.example.notificationauditservice.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalPolicies,
        long activePolicies,
        BigDecimal totalPremium,
        BigDecimal monthlyPremium,
        BigDecimal yearlyPremium,
        long totalClaims,
        long settledClaims,
        BigDecimal totalClaimedAmount,
        BigDecimal totalClaimsPaid,
        BigDecimal claimsToPremiumRatioPercent,
        List<MonthlyRevenueResponse> monthlyRevenue,
        List<BusinessAnalyticsResponse> businessAnalytics,

        // Payment-based financial analytics
        BigDecimal collectedPremium,
        BigDecimal profitBeforeOperatingExpenses
) {
}