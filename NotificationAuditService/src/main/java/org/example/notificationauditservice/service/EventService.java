package org.example.notificationauditservice.service;


import lombok.RequiredArgsConstructor;

import org.example.notificationauditservice.dto.CreateAuditRequest;
import org.example.notificationauditservice.dto.CreateNotificationRequest;
import org.example.notificationauditservice.dto.EventRequest;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class EventService {

    private final NotificationService notificationService;
    private final AuditService auditService;

    public Mono<Void> process(EventRequest request) {

        CreateAuditRequest auditRequest =
                new CreateAuditRequest(
                        request.actorUserId(),
                        request.actorRole(),
                        request.action(),
                        request.entityType(),
                        request.entityId(),
                        request.description()
                );

        CreateNotificationRequest notificationRequest =
                new CreateNotificationRequest(
                        request.notificationUserId(),
                        request.notificationType(),
                        request.notificationTitle(),
                        request.notificationMessage(),
                        request.entityType(),
                        request.entityId()
                );

        return Mono.zip(
                auditService.create(auditRequest),
                notificationService.create(notificationRequest)
        ).then();
    }
}

