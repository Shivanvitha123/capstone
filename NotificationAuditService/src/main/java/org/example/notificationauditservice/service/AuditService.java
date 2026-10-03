package org.example.notificationauditservice.service;

import lombok.RequiredArgsConstructor;

import org.example.notificationauditservice.dto.AuditResponse;
import org.example.notificationauditservice.dto.CreateAuditRequest;
import org.example.notificationauditservice.entity.AuditLog;
import org.example.notificationauditservice.repository.AuditLogRepository;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public Mono<AuditResponse> create(
            CreateAuditRequest request) {

        AuditLog auditLog = AuditLog.builder()
                .actorUserId(request.actorUserId())
                .actorRole(request.actorRole())
                .action(request.action())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .description(request.description())
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(auditLog)
                .map(this::toResponse);
    }

    public Flux<AuditResponse> getAll() {

        return repository.findAll()
                .sort(
                        (first, second) ->
                                second.getCreatedAt()
                                        .compareTo(
                                                first.getCreatedAt()
                                        )
                )
                .map(this::toResponse);
    }

    private AuditResponse toResponse(
            AuditLog auditLog) {

        return new AuditResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDescription(),
                auditLog.getCreatedAt()
        );
    }
}

