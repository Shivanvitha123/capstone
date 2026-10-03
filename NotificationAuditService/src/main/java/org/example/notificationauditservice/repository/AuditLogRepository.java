package org.example.notificationauditservice.repository;


import org.example.notificationauditservice.entity.AuditLog;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface AuditLogRepository
        extends R2dbcRepository<AuditLog, Long> {
}

