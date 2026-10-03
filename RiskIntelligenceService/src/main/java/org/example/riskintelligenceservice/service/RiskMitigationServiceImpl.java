package org.example.riskintelligenceservice.service;

import lombok.RequiredArgsConstructor;
import org.example.riskintelligenceservice.dto.RiskMitigationRequest;
import org.example.riskintelligenceservice.dto.RiskMitigationResponse;
import org.example.riskintelligenceservice.entity.RiskAssessment;
import org.example.riskintelligenceservice.entity.RiskMitigation;
import org.example.riskintelligenceservice.integration.NotificationAuditPublisher;
import org.example.riskintelligenceservice.exception.RiskAccessDeniedException;
import org.example.riskintelligenceservice.repository.RiskAssessmentRepository;
import org.example.riskintelligenceservice.repository.RiskMitigationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RiskMitigationServiceImpl
        implements RiskMitigationService {

    private final RiskMitigationRepository mitigationRepository;
    private final RiskAssessmentRepository assessmentRepository;
    private final NotificationAuditPublisher notificationAuditPublisher;

    private static final Set<String> VALID_PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final Set<String> VALID_STATUSES =
            Set.of("OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED");

    @Override
    public Mono<RiskMitigationResponse> createMitigation(
            RiskMitigationRequest request,
            Long userId,
            String role) {

        if (!"RISK_ENGINEER".equals(role)) {
            return Mono.error(new RiskAccessDeniedException());
        }

        String priority = normalize(request.priority());

        if (!VALID_PRIORITIES.contains(priority)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Priority must be LOW, MEDIUM, HIGH, or CRITICAL"
            ));
        }

        if (request.targetDate() != null
                && request.targetDate().isBefore(LocalDate.now())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Target date cannot be in the past"
            ));
        }

        return assessmentRepository.findById(request.assessmentId())
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Risk assessment not found"
                        )
                ))
                .flatMap(assessment -> {
                    LocalDateTime now = LocalDateTime.now();

                    RiskMitigation mitigation = RiskMitigation.builder()
                            .assessmentId(assessment.getId())
                            .businessId(assessment.getBusinessId())
                            .ownerId(assessment.getOwnerId())
                            .mitigationTitle(request.mitigationTitle().trim())
                            .description(request.description())
                            .priority(priority)
                            .status("OPEN")
                            .assignedTo(request.assignedTo())
                            .targetDate(request.targetDate())
                            .completedAt(null)
                            .createdBy(userId)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return mitigationRepository.save(mitigation)
                            .flatMap(saved -> notificationAuditPublisher.publish(
                                            userId,
                                            role,
                                            "RISK_MITIGATION_CREATED",
                                            "RISK_MITIGATION",
                                            saved.getId(),
                                            "Risk mitigation action created: "
                                                    + saved.getMitigationTitle()
                                                    + " (priority: "
                                                    + saved.getPriority() + ")",
                                            saved.getOwnerId(),
                                            "ACTION_REQUIRED",
                                            "New risk mitigation action",
                                            "A risk mitigation action has been created for your business: "
                                                    + saved.getMitigationTitle()
                                    )
                                    .thenReturn(toResponse(saved)));
                });
    }

    @Override
    public Flux<RiskMitigationResponse> getAllMitigations(
            Long userId,
            String role) {

        if (!canView(role)) {
            return Flux.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return mitigationRepository.findByOwnerId(userId)
                    .map(this::toResponse);
        }

        return mitigationRepository.findAll()
                .map(this::toResponse);
    }

    @Override
    public Mono<RiskMitigationResponse> getMitigation(
            Long id,
            Long userId,
            String role) {

        if (!canView(role)) {
            return Mono.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return mitigationRepository.findByIdAndOwnerId(id, userId)
                    .switchIfEmpty(Mono.error(
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Mitigation action not found"
                            )
                    ))
                    .map(this::toResponse);
        }

        return mitigationRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mitigation action not found"
                        )
                ))
                .map(this::toResponse);
    }

    @Override
    public Flux<RiskMitigationResponse> getByAssessment(
            Long assessmentId,
            Long userId,
            String role) {

        if (!canView(role)) {
            return Flux.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return assessmentRepository.findById(assessmentId)
                    .filter(assessment ->
                            userId.equals(assessment.getOwnerId()))
                    .flatMapMany(assessment ->
                            mitigationRepository
                                    .findByAssessmentId(assessmentId))
                    .map(this::toResponse);
        }

        return mitigationRepository.findByAssessmentId(assessmentId)
                .map(this::toResponse);
    }

    @Override
    public Mono<RiskMitigationResponse> updateStatus(
            Long id,
            String requestedStatus,
            Long userId,
            String role) {

        if (!canManage(role)) {
            return Mono.error(new RiskAccessDeniedException());
        }

        String newStatus = normalize(requestedStatus);

        if (!VALID_STATUSES.contains(newStatus)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be OPEN, IN_PROGRESS, COMPLETED, or CANCELLED"
            ));
        }

        return mitigationRepository.findById(id)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Mitigation action not found"
                        )
                ))
                .flatMap(mitigation -> {
                    String currentStatus = mitigation.getStatus();

                    if (!isValidTransition(currentStatus, newStatus)) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Invalid mitigation status transition from "
                                        + currentStatus + " to " + newStatus
                        ));
                    }

                    LocalDateTime now = LocalDateTime.now();

                    mitigation.setStatus(newStatus);
                    mitigation.setCompletedAt(
                            "COMPLETED".equals(newStatus) ? now : null
                    );
                    mitigation.setUpdatedAt(now);

                    return mitigationRepository.save(mitigation)
                            .flatMap(saved -> notificationAuditPublisher.publish(
                                            userId,
                                            role,
                                            "RISK_MITIGATION_STATUS_UPDATED",
                                            "RISK_MITIGATION",
                                            saved.getId(),
                                            "Risk mitigation status changed from "
                                                    + currentStatus + " to " + newStatus
                                                    + " for: " + saved.getMitigationTitle(),
                                            saved.getOwnerId(),
                                            "INFO",
                                            "Risk mitigation status updated",
                                            "The status of risk mitigation action '"
                                                    + saved.getMitigationTitle()
                                                    + "' is now " + newStatus + "."
                                    )
                                    .thenReturn(toResponse(saved)));
                });
    }

    private boolean isValidTransition(
            String currentStatus,
            String newStatus) {

        if (currentStatus == null) {
            return false;
        }

        if (currentStatus.equals(newStatus)) {
            return true;
        }

        return switch (currentStatus) {
            case "OPEN" ->
                    "IN_PROGRESS".equals(newStatus)
                            || "CANCELLED".equals(newStatus);

            case "IN_PROGRESS" ->
                    "OPEN".equals(newStatus)
                            || "COMPLETED".equals(newStatus)
                            || "CANCELLED".equals(newStatus);

            case "COMPLETED", "CANCELLED" -> false;

            default -> false;
        };
    }

    private boolean canView(String role) {
        return "ADMIN".equals(role)
                || "BUSINESS_OWNER".equals(role)
                || "UNDERWRITER".equals(role)
                || "RISK_ENGINEER".equals(role)
                || "CLAIMS_ADJUSTER".equals(role);
    }

    private boolean canManage(String role) {
        return "ADMIN".equals(role)
                || "UNDERWRITER".equals(role)
                || "RISK_ENGINEER".equals(role);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private RiskMitigationResponse toResponse(
            RiskMitigation mitigation) {

        return new RiskMitigationResponse(
                mitigation.getId(),
                mitigation.getAssessmentId(),
                mitigation.getBusinessId(),
                mitigation.getOwnerId(),
                mitigation.getMitigationTitle(),
                mitigation.getDescription(),
                mitigation.getPriority(),
                mitigation.getStatus(),
                mitigation.getAssignedTo(),
                mitigation.getTargetDate(),
                mitigation.getCompletedAt(),
                mitigation.getCreatedBy(),
                mitigation.getCreatedAt(),
                mitigation.getUpdatedAt()
        );
    }
}