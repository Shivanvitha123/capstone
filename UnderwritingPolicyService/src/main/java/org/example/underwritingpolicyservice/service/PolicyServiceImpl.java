package org.example.underwritingpolicyservice.service;

import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.CreatePolicyRequest;
import org.example.underwritingpolicyservice.dto.PolicyResponse;
import org.example.underwritingpolicyservice.dto.UpdatePolicyRequest;
import org.example.underwritingpolicyservice.entity.Policy;
import org.example.underwritingpolicyservice.exception.PolicyAccessDeniedException;
import org.example.underwritingpolicyservice.exception.PolicyAlreadyExistsException;
import org.example.underwritingpolicyservice.exception.PolicyNotFoundException;
import org.example.underwritingpolicyservice.model.PolicyStatus;
import org.example.underwritingpolicyservice.repository.PolicyRepository;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private static final Set<String> TENURE_UNITS =
            Set.of("MONTHS", "YEARS");

    private static final Set<String> PAYMENT_FREQUENCIES =
            Set.of("MONTHLY", "YEARLY");

    private final PolicyRepository policyRepository;

    // =====================================================
    // CREATE POLICY
    // =====================================================

    @Override
    public Mono<PolicyResponse> createPolicy(
            CreatePolicyRequest request,
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only BUSINESS_OWNER can create policies"
                    )
            );
        }

        if (userId == null) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        return validatePolicyTerms(
                request.startDate(),
                request.tenureValue(),
                request.tenureUnit(),
                request.paymentFrequency()
        ).flatMap(endDate ->
                policyRepository
                        .findByPolicyNumber(request.policyNumber())
                        .flatMap(existing ->
                                Mono.<PolicyResponse>error(
                                        new PolicyAlreadyExistsException(
                                                "Policy number already exists: "
                                                        + request.policyNumber()
                                        )
                                )
                        )
                        .switchIfEmpty(
                                Mono.defer(() -> {
                                    LocalDateTime now =
                                            LocalDateTime.now();

                                    Policy policy = Policy.builder()
                                            .businessId(request.businessId())
                                            .ownerId(userId)
                                            .policyNumber(request.policyNumber())
                                            .policyType(request.policyType())
                                            .coverageAmount(request.coverageAmount())
                                            .coverageDetails(
                                                    normalizeOptionalText(
                                                            request.coverageDetails()
                                                    )
                                            )
                                            .coverageLocation(
                                                    normalizeOptionalText(
                                                            request.coverageLocation()
                                                    )
                                            )
                                            .premiumAmount(request.premiumAmount())
                                            .startDate(request.startDate())
                                            .endDate(endDate)
                                            .tenureValue(request.tenureValue())
                                            .tenureUnit(
                                                    request.tenureUnit().toUpperCase()
                                            )
                                            .paymentFrequency(
                                                    request.paymentFrequency().toUpperCase()
                                            )
                                            .status(PolicyStatus.DRAFT.name())
                                            .description(request.description())
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();

                                    return policyRepository
                                            .save(policy)
                                            .map(this::toResponse);
                                })
                        )
        );
    }

    // =====================================================
    // GET POLICY
    // =====================================================

    @Override
    public Mono<PolicyResponse> getPolicy(
            Long policyId,
            Long userId,
            String role) {

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (isPrivileged(role)) {
                        return Mono.just(toResponse(policy));
                    }

                    if (isRole(role, "BUSINESS_OWNER")
                            && userId != null
                            && userId.equals(policy.getOwnerId())) {
                        return Mono.just(toResponse(policy));
                    }

                    return Mono.error(
                            new PolicyAccessDeniedException(
                                    "You do not have access to this policy"
                            )
                    );
                });
    }

    // =====================================================
    // GET OWNER POLICIES
    // =====================================================

    @Override
    public Flux<PolicyResponse> getOwnerPolicies(
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Flux.error(
                    new PolicyAccessDeniedException(
                            "Only BUSINESS_OWNER can view owner policies"
                    )
            );
        }

        if (userId == null) {
            return Flux.error(
                    new PolicyAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        return policyRepository
                .findByOwnerId(userId)
                .map(this::toResponse);
    }

    // =====================================================
    // GET ALL POLICIES
    // =====================================================

    @Override
    public Flux<PolicyResponse> getAllPolicies(
            Long userId,
            String role) {

        if (!isPrivileged(role)) {
            return Flux.error(
                    new PolicyAccessDeniedException(
                            "You do not have permission to view all policies"
                    )
            );
        }

        return policyRepository
                .findAll()
                .map(this::toResponse);
    }

    // =====================================================
    // UPDATE POLICY
    // =====================================================

    @Override
    public Mono<PolicyResponse> updatePolicy(
            Long policyId,
            UpdatePolicyRequest request,
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only BUSINESS_OWNER can update policies"
                    )
            );
        }

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (userId == null
                            || !userId.equals(policy.getOwnerId())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "You cannot update another owner's policy"
                                )
                        );
                    }

                    if (!PolicyStatus.DRAFT.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "Only DRAFT policies can be updated"
                                )
                        );
                    }

                    if (request.businessId() != null) {
                        policy.setBusinessId(request.businessId());
                    }

                    if (request.policyNumber() != null
                            && !request.policyNumber().isBlank()) {
                        policy.setPolicyNumber(
                                request.policyNumber().trim()
                        );
                    }

                    if (request.policyType() != null
                            && !request.policyType().isBlank()) {
                        policy.setPolicyType(
                                request.policyType().trim()
                        );
                    }

                    if (request.coverageAmount() != null) {
                        policy.setCoverageAmount(
                                request.coverageAmount()
                        );
                    }

                    if (request.coverageDetails() != null) {
                        policy.setCoverageDetails(
                                normalizeOptionalText(
                                        request.coverageDetails()
                                )
                        );
                    }

                    if (request.coverageLocation() != null) {
                        policy.setCoverageLocation(
                                normalizeOptionalText(
                                        request.coverageLocation()
                                )
                        );
                    }

                    if (request.premiumAmount() != null) {
                        policy.setPremiumAmount(
                                request.premiumAmount()
                        );
                    }

                    if (request.startDate() != null) {
                        policy.setStartDate(request.startDate());
                    }

                    if (request.tenureValue() != null) {
                        policy.setTenureValue(
                                request.tenureValue()
                        );
                    }

                    if (request.tenureUnit() != null) {
                        policy.setTenureUnit(
                                request.tenureUnit().toUpperCase()
                        );
                    }

                    if (request.paymentFrequency() != null) {
                        policy.setPaymentFrequency(
                                request.paymentFrequency().toUpperCase()
                        );
                    }

                    if (request.description() != null) {
                        policy.setDescription(
                                request.description()
                        );
                    }

                    return validatePolicyTerms(
                            policy.getStartDate(),
                            policy.getTenureValue(),
                            policy.getTenureUnit(),
                            policy.getPaymentFrequency()
                    ).flatMap(endDate -> {
                        policy.setEndDate(endDate);
                        policy.setUpdatedAt(LocalDateTime.now());

                        return policyRepository
                                .save(policy)
                                .map(this::toResponse);
                    });
                });
    }

    // =====================================================
    // SUBMIT POLICY
    // =====================================================

    @Override
    public Mono<PolicyResponse> submitPolicy(
            Long policyId,
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only BUSINESS_OWNER can submit policies"
                    )
            );
        }

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (userId == null
                            || !userId.equals(policy.getOwnerId())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "You cannot submit another owner's policy"
                                )
                        );
                    }

                    if (!PolicyStatus.DRAFT.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "Only DRAFT policies can be submitted"
                                )
                        );
                    }

                    policy.setStatus(
                            PolicyStatus.SUBMITTED.name()
                    );
                    policy.setUpdatedAt(LocalDateTime.now());

                    return policyRepository
                            .save(policy)
                            .map(this::toResponse);
                });
    }

    // =====================================================
    // UPDATE POLICY STATUS
    // =====================================================

    @Override
    public Mono<PolicyResponse> updatePolicyStatus(
            Long policyId,
            PolicyStatus status,
            Long userId,
            String role) {

        if (isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "BUSINESS_OWNER cannot change policy status"
                    )
            );
        }

        if (!isRole(role, "UNDERWRITER")
                && !isRole(role, "ADMIN")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only UNDERWRITER or ADMIN can change policy status"
                    )
            );
        }

        if (status == null) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Policy status is required"
                    )
            );
        }

        if (status == PolicyStatus.CANCELLED
                || status == PolicyStatus.CANCELLATION_REQUESTED) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Use the cancellation review endpoint to change cancellation status"
                    )
            );
        }

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (PolicyStatus.DRAFT.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "Policy must be submitted before status can be changed"
                                )
                        );
                    }

                    if (PolicyStatus.CANCELLATION_REQUESTED.name()
                            .equals(policy.getStatus())
                            || PolicyStatus.CANCELLED.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "Cancellation requests must be handled through the cancellation review workflow"
                                )
                        );
                    }

                    policy.setStatus(status.name());
                    policy.setUpdatedAt(LocalDateTime.now());

                    return policyRepository
                            .save(policy)
                            .map(this::toResponse);
                });
    }

    // =====================================================
    // POLICY CANCELLATION
    // =====================================================

    @Override
    public Mono<PolicyResponse> requestCancellation(
            Long policyId,
            String reason,
            Long userId,
            String role) {

        if (!isRole(role, "BUSINESS_OWNER")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only BUSINESS_OWNER can request cancellation"
                    )
            );
        }

        if (userId == null) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Authenticated user ID is required"
                    )
            );
        }

        if (reason == null || reason.isBlank()) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Cancellation reason is required"
                    )
            );
        }

        if (reason.trim().length() > 1000) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Cancellation reason cannot exceed 1000 characters"
                    )
            );
        }

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (!userId.equals(policy.getOwnerId())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "You cannot cancel another owner's policy"
                                )
                        );
                    }

                    String currentStatus = policy.getStatus();

                    if (!PolicyStatus.APPROVED.name().equals(currentStatus)
                            && !PolicyStatus.ACTIVE.name().equals(currentStatus)) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "Only APPROVED or ACTIVE policies can be submitted for cancellation"
                                )
                        );
                    }

                    policy.setPreviousStatus(currentStatus);
                    policy.setCancellationReason(reason.trim());
                    policy.setStatus(
                            PolicyStatus.CANCELLATION_REQUESTED.name()
                    );
                    policy.setUpdatedAt(LocalDateTime.now());

                    return policyRepository
                            .save(policy)
                            .map(this::toResponse);
                });
    }

    @Override
    public Mono<PolicyResponse> reviewCancellation(
            Long policyId,
            boolean approved,
            Long userId,
            String role) {

        if (!isRole(role, "UNDERWRITER")
                && !isRole(role, "ADMIN")) {
            return Mono.error(
                    new PolicyAccessDeniedException(
                            "Only UNDERWRITER or ADMIN can review cancellation requests"
                    )
            );
        }

        return policyRepository
                .findById(policyId)
                .switchIfEmpty(
                        Mono.error(
                                new PolicyNotFoundException(
                                        "Policy not found: " + policyId
                                )
                        )
                )
                .flatMap(policy -> {

                    if (!PolicyStatus.CANCELLATION_REQUESTED.name()
                            .equals(policy.getStatus())) {
                        return Mono.error(
                                new PolicyAccessDeniedException(
                                        "This policy has no pending cancellation request"
                                )
                        );
                    }

                    if (approved) {
                        policy.setStatus(
                                PolicyStatus.CANCELLED.name()
                        );
                    } else {
                        String previousStatus =
                                policy.getPreviousStatus();

                        if (previousStatus == null
                                || (!PolicyStatus.APPROVED.name()
                                .equals(previousStatus)
                                && !PolicyStatus.ACTIVE.name()
                                .equals(previousStatus))) {
                            return Mono.error(
                                    new IllegalStateException(
                                            "Previous policy status is missing or invalid"
                                    )
                            );
                        }

                        policy.setStatus(previousStatus);
                    }

                    policy.setPreviousStatus(null);
                    policy.setUpdatedAt(LocalDateTime.now());

                    return policyRepository
                            .save(policy)
                            .map(this::toResponse);
                });
    }

    // =====================================================
    // POLICY VALIDATION
    // =====================================================

    private Mono<LocalDate> validatePolicyTerms(
            LocalDate startDate,
            Integer tenureValue,
            String tenureUnit,
            String paymentFrequency) {

        if (startDate == null) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Policy start date is required"
                    )
            );
        }

        if (tenureValue == null || tenureValue < 1 || tenureValue > 120) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Tenure must be between 1 and 120"
                    )
            );
        }

        if (tenureUnit == null
                || !TENURE_UNITS.contains(tenureUnit.toUpperCase())) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Tenure unit must be MONTHS or YEARS"
                    )
            );
        }

        if (paymentFrequency == null
                || !PAYMENT_FREQUENCIES.contains(
                paymentFrequency.toUpperCase())) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Payment frequency must be MONTHLY or YEARLY"
                    )
            );
        }

        LocalDate endDate;

        try {
            if ("MONTHS".equalsIgnoreCase(tenureUnit)) {
                endDate = startDate
                        .plusMonths(tenureValue)
                        .minusDays(1);
            } else {
                endDate = startDate
                        .plusYears(tenureValue)
                        .minusDays(1);
            }
        } catch (RuntimeException exception) {
            return Mono.error(
                    new IllegalArgumentException(
                            "The supplied policy tenure is invalid"
                    )
            );
        }

        if (endDate.isBefore(startDate)) {
            return Mono.error(
                    new IllegalArgumentException(
                            "Policy end date must be after the start date"
                    )
            );
        }

        return Mono.just(endDate);
    }

    // =====================================================
    // OPTIONAL TEXT NORMALIZATION
    // =====================================================

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // =====================================================
    // ROLE HELPERS
    // =====================================================

    private boolean isPrivileged(String role) {
        return isRole(role, "ADMIN")
                || isRole(role, "UNDERWRITER")
                || isRole(role, "RISK_ENGINEER")
                || isRole(role, "CLAIMS_ADJUSTER");
    }

    private boolean isRole(
            String actualRole,
            String expectedRole) {

        return actualRole != null
                && expectedRole.equalsIgnoreCase(
                actualRole.replaceFirst("^ROLE_", "")
        );
    }

    // =====================================================
    // DTO MAPPING
    // =====================================================

    private PolicyResponse toResponse(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getBusinessId(),
                policy.getOwnerId(),
                policy.getPolicyNumber(),
                policy.getPolicyType(),
                policy.getCoverageAmount(),
                policy.getCoverageDetails(),
                policy.getCoverageLocation(),
                policy.getPremiumAmount(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getTenureValue(),
                policy.getTenureUnit(),
                policy.getPaymentFrequency(),
                PolicyStatus.valueOf(policy.getStatus()),
                policy.getDescription(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}