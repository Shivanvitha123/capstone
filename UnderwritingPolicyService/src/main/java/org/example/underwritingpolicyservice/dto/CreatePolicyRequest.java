package org.example.underwritingpolicyservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePolicyRequest(

        @NotNull(message = "Business ID is required")
        @Positive(message = "Business ID must be positive")
        Long businessId,

        @NotBlank(message = "Policy number is required")
        String policyNumber,

        @NotBlank(message = "Policy type is required")
        String policyType,

        @NotNull(message = "Coverage amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Coverage amount must be greater than zero"
        )
        BigDecimal coverageAmount,

        @Size(
                max = 1000,
                message = "Coverage details cannot exceed 1000 characters"
        )
        String coverageDetails,

        @Size(
                max = 255,
                message = "Coverage location cannot exceed 255 characters"
        )
        String coverageLocation,

        @NotNull(message = "Premium amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Premium amount must be greater than zero"
        )
        BigDecimal premiumAmount,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(
                message = "Start date cannot be in the past"
        )
        LocalDate startDate,

        @NotNull(message = "Tenure is required")
        @Min(value = 1, message = "Tenure must be at least 1")
        @Max(value = 120, message = "Tenure cannot exceed 120")
        Integer tenureValue,

        @NotBlank(message = "Tenure unit is required")
        @Pattern(
                regexp = "MONTHS|YEARS",
                message = "Tenure unit must be MONTHS or YEARS"
        )
        String tenureUnit,

        @NotBlank(message = "Payment frequency is required")
        @Pattern(
                regexp = "MONTHLY|YEARLY",
                message = "Payment frequency must be MONTHLY or YEARLY"
        )
        String paymentFrequency,

        String description
) {
}