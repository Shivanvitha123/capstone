package org.example.claimrecoveryservice.service;

import lombok.RequiredArgsConstructor;
import org.example.claimrecoveryservice.dto.ClaimAnalyticsResponse;
import org.example.claimrecoveryservice.entity.Claim;
import org.example.claimrecoveryservice.model.ClaimStatus;
import org.example.claimrecoveryservice.repository.ClaimRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimAnalyticsService {

    private final ClaimRepository claimRepository;

    public Mono<ClaimAnalyticsResponse> getClaimAnalytics() {
        return claimRepository.findAll()
                .collectList()
                .map(this::calculateAnalytics);
    }

    private ClaimAnalyticsResponse calculateAnalytics(
            List<Claim> claims) {

        long total = claims.size();

        long submitted = countByStatus(
                claims, ClaimStatus.SUBMITTED);

        long underReview = countByStatus(
                claims, ClaimStatus.UNDER_REVIEW);

        long approved = countByStatus(
                claims, ClaimStatus.APPROVED);

        long rejected = countByStatus(
                claims, ClaimStatus.REJECTED);

        long settled = countByStatus(
                claims, ClaimStatus.SETTLED);

        BigDecimal claimedAmount = claims.stream()
                .map(Claim::getClaimedAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal approvedAmount = claims.stream()
                .filter(claim ->
                        claim.getStatus() == ClaimStatus.APPROVED
                                || claim.getStatus() == ClaimStatus.SETTLED)
                .map(Claim::getApprovedAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal settledAmount = claims.stream()
                .filter(claim ->
                        claim.getStatus() == ClaimStatus.SETTLED)
                .map(Claim::getApprovedAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClaimAnalyticsResponse(
                total,
                submitted,
                underReview,
                approved,
                rejected,
                settled,
                claimedAmount,
                approvedAmount,
                settledAmount
        );
    }

    private long countByStatus(
            List<Claim> claims,
            ClaimStatus status) {

        return claims.stream()
                .filter(claim -> claim.getStatus() == status)
                .count();
    }
}