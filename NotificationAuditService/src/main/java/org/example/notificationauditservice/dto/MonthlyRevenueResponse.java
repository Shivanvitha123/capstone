package org.example.notificationauditservice.dto;

import java.math.BigDecimal;

public record MonthlyRevenueResponse(
        String month,
        BigDecimal premiumRevenue,
        long policyCount
) {}