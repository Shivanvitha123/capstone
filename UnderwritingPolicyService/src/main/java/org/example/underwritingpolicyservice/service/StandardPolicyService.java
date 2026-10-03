package org.example.underwritingpolicyservice.service;

import org.example.underwritingpolicyservice.dto.StandardPolicy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StandardPolicyService {

    private final List<StandardPolicy> standardPolicies = List.of(
            new StandardPolicy(
                    "SMALL-BUSINESS-PROPERTY",
                    "Small Business Property Protection",
                    "PROPERTY",
                    "A standard property coverage template for "
                            + "small businesses.",
                    new BigDecimal("500000"),
                    new BigDecimal("5000"),
                    "Suggested protection for business premises, "
                            + "equipment, furniture, and inventory. "
                            + "Final coverage depends on underwriting.",
                    "RETAIL"
            ),
            new StandardPolicy(
                    "SMALL-BUSINESS-LIABILITY",
                    "Small Business Liability Protection",
                    "LIABILITY",
                    "A template for businesses seeking protection "
                            + "against covered third-party liability claims.",
                    new BigDecimal("1000000"),
                    new BigDecimal("10000"),
                    "Suggested third-party liability coverage. "
                            + "Actual exclusions and limits require "
                            + "underwriting review.",
                    "GENERAL"
            ),
            new StandardPolicy(
                    "SMALL-BUSINESS-FIRE",
                    "Small Business Fire Protection",
                    "FIRE",
                    "A fire coverage template for small business "
                            + "premises and eligible business assets.",
                    new BigDecimal("500000"),
                    new BigDecimal("4500"),
                    "Suggested coverage for eligible property damage "
                            + "caused by covered fire events.",
                    "RETAIL"
            ),
            new StandardPolicy(
                    "SMALL-BUSINESS-CYBER",
                    "Small Business Cyber Protection",
                    "CYBER",
                    "A starter template for small businesses with "
                            + "digital operations and customer data.",
                    new BigDecimal("300000"),
                    new BigDecimal("6000"),
                    "Suggested coverage for eligible cyber incidents. "
                            + "Coverage depends on policy terms and "
                            + "underwriting.",
                    "IT_SERVICES"
            ),
            new StandardPolicy(
                    "SMALL-BUSINESS-COMBINED",
                    "Small Business Combined Protection",
                    "COMBINED",
                    "A combined coverage template for small businesses "
                            + "that need multiple types of protection.",
                    new BigDecimal("1000000"),
                    new BigDecimal("15000"),
                    "Suggested combination of property and liability "
                            + "coverage. Final terms require underwriting.",
                    "GENERAL"
            )
    );

    public Flux<StandardPolicy> getAllStandardPolicies() {
        return Flux.fromIterable(standardPolicies);
    }

    public Mono<StandardPolicy> getStandardPolicyById(String id) {
        return Mono.justOrEmpty(
                standardPolicies.stream()
                        .filter(policy -> policy.id().equalsIgnoreCase(id))
                        .findFirst()
        );
    }
}