package org.example.riskintelligenceservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RiskMitigationRequest(

        @NotNull(message = "Assessment ID is required")
        Long assessmentId,

        @NotBlank(message = "Mitigation title is required")
        @Size(max = 255)
        String mitigationTitle,

        @Size(max = 2000)
        String description,

        @NotBlank(message = "Priority is required")
        String priority,

        Long assignedTo,

        LocalDate targetDate
) {
}