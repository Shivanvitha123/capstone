package org.example.underwritingpolicyservice.repository;

import org.example.underwritingpolicyservice.entity.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PaymentRepository
        extends ReactiveCrudRepository<Payment, Long> {

    Flux<Payment> findByPolicyId(Long policyId);

    Flux<Payment> findByOwnerId(Long ownerId);
}