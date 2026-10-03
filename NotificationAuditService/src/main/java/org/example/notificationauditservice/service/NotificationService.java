package org.example.notificationauditservice.service;

import lombok.RequiredArgsConstructor;
import org.example.notificationauditservice.dto.CreateNotificationRequest;
import org.example.notificationauditservice.dto.NotificationResponse;
import org.example.notificationauditservice.entity.Notification;
import org.example.notificationauditservice.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public Mono<NotificationResponse> create(
            CreateNotificationRequest request
    ) {
        Notification notification = Notification.builder()
                .recipientUserId(request.recipientUserId())
                .type(request.type())
                .title(request.title())
                .message(request.message())
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        return repository.save(notification)
                .map(this::toResponse);
    }

    public Flux<NotificationResponse> getForUser(Long userId) {
        return repository
                .findByRecipientUserIdOrderByCreatedAtDesc(userId)
                .map(this::toResponse);
    }

    public Flux<NotificationResponse> getUnread(Long userId) {
        return repository
                .findByRecipientUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .map(this::toResponse);
    }

    public Mono<NotificationResponse> markRead(
            Long notificationId,
            Long userId
    ) {
        return repository.findById(notificationId)
                .switchIfEmpty(
                        Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification not found"
                                )
                        )
                )
                .flatMap(notification -> {
                    if (!userId.equals(notification.getRecipientUserId())) {
                        return Mono.error(
                                new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Notification does not belong to this user"
                                )
                        );
                    }

                    if (Boolean.TRUE.equals(notification.getRead())) {
                        return Mono.just(notification);
                    }

                    notification.setRead(true);
                    return repository.save(notification);
                })
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(
            Notification notification
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getRead(),
                notification.getCreatedAt()
        );
    }
}