package org.example.notificationauditservice.dto;

import java.time.LocalDateTime;

public record AuditResponse(

        Long id,
        Long actorUserId,
        String actorRole,
        String action,
        String entityType,
        Long entityId,
        String description,
        LocalDateTime createdAt
) {
}

