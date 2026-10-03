package org.example.underwritingpolicyservice.controller;

import lombok.RequiredArgsConstructor;
import org.example.underwritingpolicyservice.dto.StandardPolicy;
import org.example.underwritingpolicyservice.service.StandardPolicyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/policies/standard")
@RequiredArgsConstructor
public class StandardPolicyController {

    private final StandardPolicyService standardPolicyService;

    @GetMapping
    public Flux<StandardPolicy> getAllStandardPolicies() {
        return standardPolicyService.getAllStandardPolicies();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<StandardPolicy>> getStandardPolicy(
            @PathVariable String id) {

        return standardPolicyService
                .getStandardPolicyById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}