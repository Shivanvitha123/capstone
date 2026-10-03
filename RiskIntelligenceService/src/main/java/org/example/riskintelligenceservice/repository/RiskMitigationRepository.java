package org.example.riskintelligenceservice.repository;

import org.example.riskintelligenceservice.entity.RiskMitigation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RiskMitigationRepository
        extends ReactiveCrudRepository<RiskMitigation, Long> {

    Flux<RiskMitigation> findByAssessmentId(Long assessmentId);

    Flux<RiskMitigation> findByBusinessId(Long businessId);

    Flux<RiskMitigation> findByOwnerId(Long ownerId);

    Mono<RiskMitigation> findByIdAndOwnerId(
            Long id,
            Long ownerId
    );
}