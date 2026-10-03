package org.example.underwritingpolicyservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("policy_payments")
public class Payment {

    @Id
    private Long id;

    @Column("policy_id")
    private Long policyId;

    @Column("owner_id")
    private Long ownerId;

    @Column("amount")
    private BigDecimal amount;

    @Column("payment_date")
    private LocalDateTime paymentDate;

    @Column("status")
    private String status;

    @Column("transaction_reference")
    private String transactionReference;

    @Column("created_at")
    private LocalDateTime createdAt;
}