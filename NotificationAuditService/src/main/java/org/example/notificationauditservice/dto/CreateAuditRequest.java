package org.example.notificationauditservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAuditRequest(

        @NotNull
        Long actorUserId,

        String actorRole,

        @NotBlank
        String action,

        @NotBlank
        String entityType,

        Long entityId,

        String description
) {
}
