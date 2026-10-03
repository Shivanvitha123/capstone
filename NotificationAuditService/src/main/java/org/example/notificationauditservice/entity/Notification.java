package org.example.notificationauditservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.notificationauditservice.model.NotificationType;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class Notification {

    @Id
    private Long id;

    @Column("recipient_user_id")
    private Long recipientUserId;

    @Column("type")
    private NotificationType type;

    @Column("title")
    private String title;

    @Column("message")
    private String message;

    @Column("reference_type")
    private String referenceType;

    @Column("reference_id")
    private Long referenceId;

    @Column("is_read")
    private Boolean read;

    @Column("created_at")
    private LocalDateTime createdAt;
}

