package org.example.riskintelligenceservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record RiskAssessmentRequest(

        @NotNull(message = "Business ID is required")
        Long businessId,

        Long policyId,

        @NotNull(message = "Industry is required")
        String industry,

        @NotNull(message = "Annual revenue is required")
        @DecimalMin(value = "0.0", message = "Annual revenue cannot be negative")
        BigDecimal annualRevenue,

        @NotNull(message = "Employee count is required")
        @PositiveOrZero(message = "Employee count cannot be negative")
        Integer employeeCount,

        @NotNull(message = "Branch count is required")
        @PositiveOrZero(message = "Branch count cannot be negative")
        Integer branchCount,

        String premisesType,

        @NotNull(message = "Previous insurance claims count is required")
        @PositiveOrZero(message = "Previous claims cannot be negative")
        Integer previousInsuranceClaims,

        Boolean existingInsurance
) {
}