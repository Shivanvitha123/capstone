package org.example.notificationauditservice.dto;

import org.example.notificationauditservice.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(

        Long id,
        Long recipientUserId,
        NotificationType type,
        String title,
        String message,
        String referenceType,
        Long referenceId,
        Boolean read,
        LocalDateTime createdAt
) {
}

