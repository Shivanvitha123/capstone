package org.example.riskintelligenceservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RiskMitigationStatusRequest(

        @NotBlank(message = "Status is required")
        String status
) {
}