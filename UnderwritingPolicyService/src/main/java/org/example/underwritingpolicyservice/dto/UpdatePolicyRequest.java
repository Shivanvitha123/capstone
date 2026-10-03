package org.example.underwritingpolicyservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePolicyRequest(

        @Positive(message = "Business ID must be positive")
        Long businessId,

        String policyNumber,

        String policyType,

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

        @DecimalMin(
                value = "0.01",
                message = "Premium amount must be greater than zero"
        )
        BigDecimal premiumAmount,

        LocalDate startDate,

        @Min(value = 1, message = "Tenure must be at least 1")
        @Max(value = 120, message = "Tenure cannot exceed 120")
        Integer tenureValue,

        @Pattern(
                regexp = "MONTHS|YEARS",
                message = "Tenure unit must be MONTHS or YEARS"
        )
        String tenureUnit,

        @Pattern(
                regexp = "MONTHLY|YEARLY",
                message = "Payment frequency must be MONTHLY or YEARLY"
        )
        String paymentFrequency,

        String description
) {
}