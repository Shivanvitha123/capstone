package org.example.notificationauditservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("audit_logs")
public class AuditLog {

    @Id
    private Long id;

    @Column("actor_user_id")
    private Long actorUserId;

    @Column("actor_role")
    private String actorRole;

    @Column("action")
    private String action;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private Long entityId;

    @Column("description")
    private String description;

    @Column("created_at")
    private LocalDateTime createdAt;
}

