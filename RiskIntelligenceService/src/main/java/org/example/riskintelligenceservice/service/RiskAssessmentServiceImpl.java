package org.example.riskintelligenceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.example.riskintelligenceservice.dto.RiskAssessmentRequest;
import org.example.riskintelligenceservice.dto.RiskAssessmentResponse;
import org.example.riskintelligenceservice.entity.RiskAssessment;
import org.example.riskintelligenceservice.integration.NotificationAuditPublisher;
import org.example.riskintelligenceservice.exception.RiskAccessDeniedException;
import org.example.riskintelligenceservice.exception.RiskAssessmentNotFoundException;
import org.example.riskintelligenceservice.model.RiskLevel;
import org.example.riskintelligenceservice.repository.RiskAssessmentRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskAssessmentServiceImpl
        implements RiskAssessmentService {

    private final RiskAssessmentRepository repository;
    private final WebClient businessWebClient;
    private final NotificationAuditPublisher notificationAuditPublisher;

    @Override
    public Mono<RiskAssessmentResponse> createAssessment(
            RiskAssessmentRequest request,
            Long userId,
            String role,
            String authorization) {

        if (!"RISK_ENGINEER".equals(role)) {
            return Mono.error(new RiskAccessDeniedException());
        }

        if (authorization == null || authorization.isBlank()) {
            return Mono.error(new RiskAccessDeniedException());
        }

        return businessWebClient.get()
                .uri("/api/business/{businessId}", request.businessId())
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(business -> {

                    JsonNode ownerNode = business.get("ownerId");

                    if (ownerNode == null
                            || !ownerNode.canConvertToLong()) {

                        return Mono.error(
                                new IllegalStateException(
                                        "Business response does not contain a valid ownerId"
                                )
                        );
                    }

                    Long businessOwnerId = ownerNode.longValue();

                    if (businessOwnerId <= 0) {
                        return Mono.error(
                                new IllegalStateException(
                                        "Business owner ID must be positive"
                                )
                        );
                    }

                    BigDecimal riskScore =
                            calculateRiskScore(request);

                    RiskLevel riskLevel =
                            calculateRiskLevel(riskScore);

                    String riskFactors =
                            buildRiskFactors(request);

                    String recommendation =
                            buildRecommendation(
                                    request,
                                    riskLevel
                            );

                    LocalDateTime now = LocalDateTime.now();

                    RiskAssessment assessment =
                            RiskAssessment.builder()
                                    .businessId(request.businessId())
                                    .policyId(request.policyId())
                                    .ownerId(businessOwnerId)
                                    .riskScore(riskScore)
                                    .riskLevel(riskLevel)
                                    .riskFactors(riskFactors)
                                    .recommendation(recommendation)
                                    .assessedBy(userId)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build();

                    return repository.save(assessment)
                            .flatMap(saved -> notificationAuditPublisher.publish(
                                            userId,
                                            role,
                                            "RISK_ASSESSMENT_CREATED",
                                            "RISK_ASSESSMENT",
                                            saved.getId(),
                                            "Risk assessment created for business "
                                                    + saved.getBusinessId()
                                                    + " with score "
                                                    + saved.getRiskScore()
                                                    + " and level "
                                                    + saved.getRiskLevel(),
                                            saved.getOwnerId(),
                                            "INFO",
                                            "Risk assessment completed",
                                            "A risk assessment for your business has been completed. Risk level: "
                                                    + saved.getRiskLevel()
                                                    + ", score: "
                                                    + saved.getRiskScore()
                                    )
                                    .thenReturn(toResponse(saved)));
                });
    }

    @Override
    public Flux<RiskAssessmentResponse> getAllAssessments(
            Long userId,
            String role) {

        if (!canViewAssessments(role)) {
            return Flux.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return repository.findByOwnerId(userId)
                    .map(this::toResponse);
        }

        return repository.findAll()
                .map(this::toResponse);
    }

    @Override
    public Mono<RiskAssessmentResponse> getAssessment(
            Long id,
            Long userId,
            String role) {

        if (!canViewAssessments(role)) {
            return Mono.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return repository.findByIdAndOwnerId(id, userId)
                    .switchIfEmpty(
                            Mono.error(
                                    new RiskAssessmentNotFoundException(id)
                            )
                    )
                    .map(this::toResponse);
        }

        return repository.findById(id)
                .switchIfEmpty(
                        Mono.error(
                                new RiskAssessmentNotFoundException(id)
                        )
                )
                .map(this::toResponse);
    }

    @Override
    public Flux<RiskAssessmentResponse> getByBusiness(
            Long businessId,
            Long userId,
            String role) {

        if (!canViewAssessments(role)) {
            return Flux.error(new RiskAccessDeniedException());
        }

        if ("BUSINESS_OWNER".equals(role)) {
            return repository.findByBusinessId(businessId)
                    .filter(assessment ->
                            userId.equals(assessment.getOwnerId()))
                    .map(this::toResponse);
        }

        return repository.findByBusinessId(businessId)
                .map(this::toResponse);
    }

    /*
     * Multifactor risk scoring.
     *
     * This is a deterministic rule-based model, not a
     * statistically trained insurance risk model.
     */
    private BigDecimal calculateRiskScore(
            RiskAssessmentRequest request) {

        double score = 20.0;

        // Factor 1: Industry risk
        String industry = normalize(request.industry());

        score += switch (industry) {
            case "CONSTRUCTION",
                 "MANUFACTURING",
                 "TRANSPORTATION",
                 "MINING",
                 "CHEMICALS" -> 20.0;

            case "HOSPITALITY",
                 "RETAIL",
                 "FOOD",
                 "LOGISTICS",
                 "HEALTHCARE" -> 12.0;

            case "INFORMATION TECHNOLOGY",
                 "IT",
                 "SOFTWARE",
                 "CONSULTING",
                 "EDUCATION" -> 5.0;

            default -> 10.0;
        };

        // Factor 2: Previous insurance claims
        int claims = request.previousInsuranceClaims();

        if (claims >= 5) {
            score += 25.0;
        } else if (claims >= 3) {
            score += 18.0;
        } else if (claims == 2) {
            score += 12.0;
        } else if (claims == 1) {
            score += 7.0;
        }

        // Factor 3: Number of branches
        int branches = request.branchCount();

        if (branches >= 20) {
            score += 15.0;
        } else if (branches >= 10) {
            score += 12.0;
        } else if (branches >= 5) {
            score += 8.0;
        } else if (branches >= 2) {
            score += 4.0;
        }

        // Factor 4: Premises type
        String premises = normalize(request.premisesType());

        score += switch (premises) {
            case "OWNED" -> 2.0;
            case "RENTED" -> 5.0;
            case "LEASED" -> 6.0;
            case "SHARED" -> 8.0;
            case "HOME_BASED" -> 3.0;
            case "OTHER" -> 7.0;
            default -> 5.0;
        };

        // Factor 5: Employee count
        int employees = request.employeeCount();

        if (employees >= 500) {
            score += 15.0;
        } else if (employees >= 200) {
            score += 12.0;
        } else if (employees >= 50) {
            score += 8.0;
        } else if (employees >= 10) {
            score += 4.0;
        }

        // Factor 6: Annual revenue
        BigDecimal revenue = request.annualRevenue();

        if (revenue.compareTo(new BigDecimal("100000000")) >= 0) {
            score += 12.0;
        } else if (revenue.compareTo(new BigDecimal("50000000")) >= 0) {
            score += 9.0;
        } else if (revenue.compareTo(new BigDecimal("10000000")) >= 0) {
            score += 6.0;
        } else if (revenue.compareTo(new BigDecimal("1000000")) >= 0) {
            score += 3.0;
        }

        // Factor 7: Existing insurance
        if (Boolean.TRUE.equals(request.existingInsurance())) {
            score -= 5.0;
        } else {
            score += 5.0;
        }

        // Keep score within the valid 0-100 range.
        score = Math.max(0.0, Math.min(100.0, score));

        return BigDecimal.valueOf(score)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private RiskLevel calculateRiskLevel(BigDecimal score) {

        if (score.compareTo(BigDecimal.valueOf(25)) < 0) {
            return RiskLevel.LOW;
        }

        if (score.compareTo(BigDecimal.valueOf(50)) < 0) {
            return RiskLevel.MEDIUM;
        }

        if (score.compareTo(BigDecimal.valueOf(75)) < 0) {
            return RiskLevel.HIGH;
        }

        return RiskLevel.CRITICAL;
    }

    private String buildRiskFactors(
            RiskAssessmentRequest request) {

        List<String> factors = new ArrayList<>();

        factors.add("Industry: " + request.industry());
        factors.add("Annual revenue: " + request.annualRevenue());
        factors.add("Employees: " + request.employeeCount());
        factors.add("Branches: " + request.branchCount());
        factors.add("Premises type: " + request.premisesType());

        factors.add(
                "Previous insurance claims: "
                        + request.previousInsuranceClaims()
        );

        factors.add(
                "Existing insurance: "
                        + (Boolean.TRUE.equals(request.existingInsurance())
                        ? "Yes" : "No")
        );

        return String.join("; ", factors);
    }

    private String buildRecommendation(
            RiskAssessmentRequest request,
            RiskLevel riskLevel) {

        List<String> recommendations = new ArrayList<>();

        if (request.previousInsuranceClaims() >= 2) {
            recommendations.add(
                    "Review previous claims and implement corrective measures."
            );
        }

        if (request.branchCount() >= 5) {
            recommendations.add(
                    "Conduct regular risk inspections across all branches."
            );
        }

        if (request.employeeCount() >= 50) {
            recommendations.add(
                    "Maintain employee safety training and documented procedures."
            );
        }

        if ("CONSTRUCTION".equals(normalize(request.industry()))
                || "MANUFACTURING".equals(normalize(request.industry()))
                || "MINING".equals(normalize(request.industry()))) {

            recommendations.add(
                    "Review workplace safety controls and equipment maintenance."
            );
        }

        if (!Boolean.TRUE.equals(request.existingInsurance())) {
            recommendations.add(
                    "Review suitable insurance coverage for the business."
            );
        }

        switch (riskLevel) {
            case LOW -> recommendations.add(
                    "Maintain current controls and perform periodic risk reviews."
            );

            case MEDIUM -> recommendations.add(
                    "Review identified risk factors and strengthen preventive controls."
            );

            case HIGH -> recommendations.add(
                    "Prioritize risk mitigation and schedule a detailed risk review."
            );

            case CRITICAL -> recommendations.add(
                    "Escalate for urgent review and prepare a documented mitigation plan."
            );
        }

        return String.join(" ", recommendations);
    }

    private boolean canViewAssessments(String role) {
        return "ADMIN".equals(role)
                || "BUSINESS_OWNER".equals(role)
                || "UNDERWRITER".equals(role)
                || "RISK_ENGINEER".equals(role)
                || "CLAIMS_ADJUSTER".equals(role);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase();
    }

    private RiskAssessmentResponse toResponse(
            RiskAssessment assessment) {

        return new RiskAssessmentResponse(
                assessment.getId(),
                assessment.getBusinessId(),
                assessment.getPolicyId(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getRiskFactors(),
                assessment.getRecommendation(),
                assessment.getAssessedBy(),
                assessment.getCreatedAt(),
                assessment.getUpdatedAt()
        );
    }
}