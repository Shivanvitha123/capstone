package org.example.riskintelligenceservice.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RiskMitigationResponse(
        Long id,
        Long assessmentId,
        Long businessId,
        Long ownerId,
        String mitigationTitle,
        String description,
        String priority,
        String status,
        Long assignedTo,
        LocalDate targetDate,
        LocalDateTime completedAt,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}