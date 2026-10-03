package org.example.notificationauditservice.repository;

import org.example.notificationauditservice.entity.Notification;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

import reactor.core.publisher.Flux;

public interface NotificationRepository
        extends R2dbcRepository<Notification, Long> {

    Flux<Notification> findByRecipientUserIdOrderByCreatedAtDesc(
            Long recipientUserId
    );

    Flux<Notification> findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(
            Long recipientUserId
    );
}


