package org.example.underwritingpolicyservice.dto;

import org.example.underwritingpolicyservice.model.PolicyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolicyResponse(

        Long id,

        Long businessId,

        Long ownerId,

        String policyNumber,

        String policyType,

        BigDecimal coverageAmount,

        String coverageDetails,

        String coverageLocation,

        BigDecimal premiumAmount,

        LocalDate startDate,

        LocalDate endDate,

        Integer tenureValue,

        String tenureUnit,

        String paymentFrequency,

        PolicyStatus status,

        String description,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}