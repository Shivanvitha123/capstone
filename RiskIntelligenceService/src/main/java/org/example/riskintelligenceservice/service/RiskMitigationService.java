package org.example.riskintelligenceservice.service;

import org.example.riskintelligenceservice.dto.RiskMitigationRequest;
import org.example.riskintelligenceservice.dto.RiskMitigationResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RiskMitigationService {

    Mono<RiskMitigationResponse> createMitigation(
            RiskMitigationRequest request,
            Long userId,
            String role
    );

    Flux<RiskMitigationResponse> getAllMitigations(
            Long userId,
            String role
    );

    Mono<RiskMitigationResponse> getMitigation(
            Long id,
            Long userId,
            String role
    );

    Flux<RiskMitigationResponse> getByAssessment(
            Long assessmentId,
            Long userId,
            String role
    );

    Mono<RiskMitigationResponse> updateStatus(
            Long id,
            String status,
            Long userId,
            String role
    );
}