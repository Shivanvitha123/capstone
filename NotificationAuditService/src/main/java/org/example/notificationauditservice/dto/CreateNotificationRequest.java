package org.example.notificationauditservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.example.notificationauditservice.model.NotificationType;

public record CreateNotificationRequest(

        @NotNull
        Long recipientUserId,

        @NotNull
        NotificationType type,

        @NotBlank
        String title,

        @NotBlank
        String message,

        String referenceType,

        Long referenceId
) {
}

