package org.example.underwritingpolicyservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        Long policyId,
        Long ownerId,
        BigDecimal amount,
        LocalDateTime paymentDate,
        String status,
        String transactionReference,
        LocalDateTime createdAt
) {
}