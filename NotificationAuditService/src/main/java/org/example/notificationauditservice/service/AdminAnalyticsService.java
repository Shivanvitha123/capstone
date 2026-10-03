package org.example.notificationauditservice.service;

import org.example.notificationauditservice.dto.AdminDashboardResponse;
import org.example.notificationauditservice.dto.BusinessAnalyticsResponse;
import org.example.notificationauditservice.dto.MonthlyRevenueResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

@Service
public class AdminAnalyticsService {

    @Value("${analytics.services.policy-url}")
    private String policyServiceUrl;

    @Value("${analytics.services.claims-url}")
    private String claimsServiceUrl;

    public AdminAnalyticsService(WebClient.Builder webClientBuilder) {
        // Retained for compatibility with the existing configuration.
    }

    public Mono<AdminDashboardResponse> getDashboard(String authorization) {

        // Retrieve policies from Policy Service
        Mono<List<PolicyData>> policiesMono =
                WebClient.create(policyServiceUrl)
                        .get()
                        .uri("/api/policies")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .retrieve()
                        .bodyToFlux(PolicyData.class)
                        .collectList();

        // Retrieve claims from Claims Recovery Service
        Mono<List<ClaimData>> claimsMono =
                WebClient.create(claimsServiceUrl)
                        .get()
                        .uri("/api/claims")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .retrieve()
                        .bodyToFlux(ClaimData.class)
                        .collectList();

        // Retrieve payment records from Policy Service
        Mono<List<PaymentData>> paymentsMono =
                WebClient.create(policyServiceUrl)
                        .get()
                        .uri("/api/payments")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .retrieve()
                        .bodyToFlux(PaymentData.class)
                        .collectList();

        // Calculate dashboard only after all three responses arrive
        return Mono.zip(policiesMono, claimsMono, paymentsMono)
                .map(data -> calculateDashboard(
                        data.getT1(),
                        data.getT2(),
                        data.getT3()
                ));
    }

    private AdminDashboardResponse calculateDashboard(
            List<PolicyData> policies,
            List<ClaimData> claims,
            List<PaymentData> payments) {

        LocalDate today = LocalDate.now();

        long totalPolicies = policies.size();

        long activePolicies = policies.stream()
                .filter(p -> "ACTIVE".equalsIgnoreCase(p.status()))
                .count();

        BigDecimal totalPremium = policies.stream()
                .map(p -> safe(p.premiumAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyPremium = policies.stream()
                .filter(p -> p.startDate() != null
                        && YearMonth.from(p.startDate())
                        .equals(YearMonth.from(today)))
                .map(p -> safe(p.premiumAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearlyPremium = policies.stream()
                .filter(p -> p.startDate() != null
                        && p.startDate().getYear() == today.getYear())
                .map(p -> safe(p.premiumAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalClaims = claims.size();

        long settledClaims = claims.stream()
                .filter(this::isSettled)
                .count();

        BigDecimal totalClaimedAmount = claims.stream()
                .map(c -> safe(c.claimedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalClaimsPaid = claims.stream()
                .filter(this::isSettled)
                .map(c -> safe(c.approvedAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Sum only successful payment records
        BigDecimal collectedPremium = payments.stream()
                .filter(this::isSuccessfulPayment)
                .map(p -> safe(p.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Profit before operating expenses
        BigDecimal profitBeforeOperatingExpenses =
                collectedPremium.subtract(totalClaimsPaid);

        // Claims-to-premium ratio based on actual collected premiums
        BigDecimal claimsToPremiumRatio = BigDecimal.ZERO;

        if (collectedPremium.compareTo(BigDecimal.ZERO) > 0) {
            claimsToPremiumRatio = totalClaimsPaid
                    .multiply(BigDecimal.valueOf(100))
                    .divide(
                            collectedPremium,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        // Existing monthly revenue remains based on policy start dates
        List<MonthlyRevenueResponse> monthlyRevenue =
                calculateMonthlyRevenue(policies);

        List<BusinessAnalyticsResponse> businessAnalytics =
                calculateBusinessAnalytics(policies, claims);

        return new AdminDashboardResponse(
                totalPolicies,
                activePolicies,
                totalPremium,
                monthlyPremium,
                yearlyPremium,
                totalClaims,
                settledClaims,
                totalClaimedAmount,
                totalClaimsPaid,
                claimsToPremiumRatio,
                monthlyRevenue,
                businessAnalytics,
                collectedPremium,
                profitBeforeOperatingExpenses
        );
    }

    private List<MonthlyRevenueResponse> calculateMonthlyRevenue(
            List<PolicyData> policies) {

        Map<YearMonth, List<PolicyData>> monthlyPolicies =
                new LinkedHashMap<>();

        for (PolicyData policy : policies) {
            if (policy.startDate() == null) {
                continue;
            }

            YearMonth month = YearMonth.from(policy.startDate());

            monthlyPolicies.computeIfAbsent(
                    month,
                    key -> new ArrayList<>()
            ).add(policy);
        }

        return monthlyPolicies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    BigDecimal revenue = entry.getValue().stream()
                            .map(p -> safe(p.premiumAmount()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new MonthlyRevenueResponse(
                            entry.getKey().toString(),
                            revenue,
                            entry.getValue().size()
                    );
                })
                .toList();
    }

    private List<BusinessAnalyticsResponse> calculateBusinessAnalytics(
            List<PolicyData> policies,
            List<ClaimData> claims) {

        Map<Long, List<PolicyData>> policiesByBusiness =
                new LinkedHashMap<>();

        Map<Long, List<ClaimData>> claimsByBusiness =
                new LinkedHashMap<>();

        policies.stream()
                .filter(p -> p.businessId() != null)
                .forEach(p -> policiesByBusiness
                        .computeIfAbsent(
                                p.businessId(),
                                key -> new ArrayList<>()
                        )
                        .add(p));

        claims.stream()
                .filter(c -> c.businessId() != null)
                .forEach(c -> claimsByBusiness
                        .computeIfAbsent(
                                c.businessId(),
                                key -> new ArrayList<>()
                        )
                        .add(c));

        return java.util.stream.Stream.concat(
                        policiesByBusiness.keySet().stream(),
                        claimsByBusiness.keySet().stream()
                )
                .distinct()
                .sorted()
                .map(businessId -> {
                    List<PolicyData> businessPolicies =
                            policiesByBusiness.getOrDefault(
                                    businessId,
                                    List.of()
                            );

                    List<ClaimData> businessClaims =
                            claimsByBusiness.getOrDefault(
                                    businessId,
                                    List.of()
                            );

                    long businessActivePolicies =
                            businessPolicies.stream()
                                    .filter(p -> "ACTIVE".equalsIgnoreCase(
                                            p.status()
                                    ))
                                    .count();

                    BigDecimal businessPremium =
                            businessPolicies.stream()
                                    .map(p -> safe(p.premiumAmount()))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal businessClaimsPaid =
                            businessClaims.stream()
                                    .filter(this::isSettled)
                                    .map(c -> safe(c.approvedAmount()))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new BusinessAnalyticsResponse(
                            businessId,
                            businessPolicies.size(),
                            businessActivePolicies,
                            businessPremium,
                            businessClaims.size(),
                            businessClaimsPaid
                    );
                })
                .toList();
    }

    private boolean isSettled(ClaimData claim) {
        return "SETTLED".equalsIgnoreCase(claim.status())
                || "PAID".equalsIgnoreCase(claim.status());
    }

    private boolean isSuccessfulPayment(PaymentData payment) {
        return payment.status() != null
                && "SUCCESS".equalsIgnoreCase(payment.status().trim());
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record PolicyData(
            Long id,
            Long businessId,
            Long ownerId,
            String policyNumber,
            String policyType,
            BigDecimal coverageAmount,
            BigDecimal premiumAmount,
            LocalDate startDate,
            LocalDate endDate,
            Integer tenureValue,
            String tenureUnit,
            String paymentFrequency,
            String status,
            String description
    ) {
    }

    private record ClaimData(
            Long id,
            Long businessId,
            Long policyId,
            Long ownerId,
            String claimNumber,
            String claimType,
            LocalDate incidentDate,
            LocalDate reportedDate,
            BigDecimal claimedAmount,
            BigDecimal approvedAmount,
            String description,
            String status,
            Long assignedAdjusterId
    ) {
    }

    private record PaymentData(
            Long id,
            Long policyId,
            Long ownerId,
            BigDecimal amount,
            LocalDate paymentDate,
            String status,
            String transactionReference,
            LocalDate createdAt
    ) {
    }
}