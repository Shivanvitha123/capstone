package org.example.notificationauditservice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.example.notificationauditservice.model.NotificationType;

public record EventRequest(

        @NotNull
        Long actorUserId,

        String actorRole,

        @NotBlank
        String action,

        @NotBlank
        String entityType,

        Long entityId,

        String description,

        @NotNull
        Long notificationUserId,

        @NotNull
        NotificationType notificationType,

        @NotBlank
        String notificationTitle,

        @NotBlank
        String notificationMessage
) {
}

