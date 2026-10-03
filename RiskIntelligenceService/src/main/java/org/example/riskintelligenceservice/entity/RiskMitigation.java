package org.example.riskintelligenceservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("risk_mitigations")
public class RiskMitigation {

    @Id
    private Long id;

    @Column("assessment_id")
    private Long assessmentId;

    @Column("business_id")
    private Long businessId;

    @Column("owner_id")
    private Long ownerId;

    @Column("mitigation_title")
    private String mitigationTitle;

    @Column("description")
    private String description;

    @Column("priority")
    private String priority;

    @Column("status")
    private String status;

    @Column("assigned_to")
    private Long assignedTo;

    @Column("target_date")
    private LocalDate targetDate;

    @Column("completed_at")
    private LocalDateTime completedAt;

    @Column("created_by")
    private Long createdBy;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;
}