package org.example.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class RbacAuthorizationFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(RbacAuthorizationFilter.class);

    // =========================================================
    // ROLE GROUPS
    // =========================================================

    private static final Set<String> ADMIN =
            Set.of("ADMIN");

    private static final Set<String> OWNER =
            Set.of("BUSINESS_OWNER");

    private static final Set<String> UNDERWRITING =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER"
            );

    private static final Set<String> BUSINESS_READ =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER",
                    "RISK_ENGINEER"
            );

    private static final Set<String> POLICY_READ =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER",
                    "RISK_ENGINEER",
                    "CLAIMS_ADJUSTER"
            );

    /*
     * Users who can view an individual policy.
     */
    private static final Set<String> POLICY_DETAILS =
            Set.of(
                    "ADMIN",
                    "BUSINESS_OWNER",
                    "UNDERWRITER",
                    "RISK_ENGINEER",
                    "CLAIMS_ADJUSTER"
            );

    /*
     * Users who can view standard/preloaded policy templates.
     *
     * Business Owner is included because the Create Policy
     * screen loads these templates.
     */
    private static final Set<String> STANDARD_POLICY_READ =
            Set.of(
                    "ADMIN",
                    "BUSINESS_OWNER",
                    "UNDERWRITER",
                    "RISK_ENGINEER",
                    "CLAIMS_ADJUSTER"
            );

    private static final Set<String> RISK_READ =
            Set.of(
                    "ADMIN",
                    "BUSINESS_OWNER",
                    "UNDERWRITER",
                    "RISK_ENGINEER",
                    "CLAIMS_ADJUSTER"
            );

    private static final Set<String> RISK_ANALYST =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER",
                    "RISK_ENGINEER"
            );

    private static final Set<String> RISK_MITIGATION_MANAGE =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER",
                    "RISK_ENGINEER"
            );

    private static final Set<String> CLAIMS_READ =
            Set.of(
                    "ADMIN",
                    "UNDERWRITER",
                    "CLAIMS_ADJUSTER",
                    "RISK_ENGINEER"
            );

    private static final Set<String> CLAIM_DETAILS =
            Set.of(
                    "ADMIN",
                    "BUSINESS_OWNER",
                    "UNDERWRITER",
                    "CLAIMS_ADJUSTER",
                    "RISK_ENGINEER"
            );

    private static final Set<String> CLAIMS_MANAGE =
            Set.of(
                    "ADMIN",
                    "CLAIMS_ADJUSTER"
            );

    private static final Pattern ID =
            Pattern.compile("\\d+");

    // =========================================================
    // MAIN FILTER
    // =========================================================

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        /*
         * Remove trailing slash so:
         *
         * /api/policies/standard/
         *
         * becomes:
         *
         * /api/policies/standard
         */
        if (path.length() > 1
                && path.endsWith("/")) {

            path = path.substring(
                    0,
                    path.length() - 1
            );
        }

        HttpMethod method =
                exchange.getRequest().getMethod();

        /*
         * Allow CORS preflight and public endpoints.
         */
        if (method == HttpMethod.OPTIONS
                || isPublicPath(path)) {

            return chain.filter(exchange);
        }

        /*
         * Read role inserted by the authentication/JWT filter.
         */
        String role = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Role");

        if (role == null || role.isBlank()) {

            log.warn(
                    "RBAC denied: missing role, method={}, path={}",
                    method,
                    path
            );

            return forbidden(
                    exchange,
                    "Authenticated user role is missing"
            );
        }

        /*
         * Normalize role.
         *
         * Supports:
         *
         * BUSINESS_OWNER
         *
         * and:
         *
         * ROLE_BUSINESS_OWNER
         */
        role = role.trim()
                .toUpperCase()
                .replaceFirst(
                        "^ROLE_",
                        ""
                );

        if (isAllowed(
                path,
                method,
                role
        )) {

            log.debug(
                    "RBAC allowed role={} method={} path={}",
                    role,
                    method,
                    path
            );

            return chain.filter(exchange);
        }

        log.warn(
                "RBAC denied role={} method={} path={}",
                role,
                method,
                path
        );

        return forbidden(
                exchange,
                "You do not have permission to perform this operation"
        );
    }

    // =========================================================
    // AUTHORIZATION RULES
    // =========================================================

    private boolean isAllowed(
            String path,
            HttpMethod method,
            String role) {

        // =====================================================
        // IDENTITY
        // =====================================================

        /*
         * Current authenticated user.
         */
        if (path.equals("/api/auth/me")) {
            return true;
        }

        /*
         * LOGOUT
         *
         * Any authenticated role is allowed to logout.
         */
        if (path.equals("/api/auth/logout")
                && method == HttpMethod.POST) {

            return true;
        }

        /*
         * ADMIN USER LIST
         */
        if (path.equals("/api/auth/users")
                && method == HttpMethod.GET) {

            return ADMIN.contains(role);
        }

        /*
         * ADMIN USER DETAILS
         */
        if (path.matches(
                "^/api/auth/users/[^/]+$"
        ) && method == HttpMethod.GET) {

            return ADMIN.contains(role);
        }

        // =====================================================
        // ADMIN ANALYTICS
        // =====================================================

        if (path.equals(
                "/api/admin/analytics/dashboard"
        ) && method == HttpMethod.GET) {

            return ADMIN.contains(role);
        }

        // =====================================================
        // BUSINESS
        // =====================================================

        if (path.equals("/api/business")) {

            /*
             * BUSINESS OWNER creates business.
             */
            if (method == HttpMethod.POST) {

                return OWNER.contains(role);
            }

            /*
             * Admin / Underwriter / Risk Engineer
             * can view businesses.
             */
            if (method == HttpMethod.GET) {

                return BUSINESS_READ.contains(role);
            }

            return false;
        }

        /*
         * Business Owner's own business.
         */
        if (path.equals("/api/business/me")
                && method == HttpMethod.GET) {

            return OWNER.contains(role);
        }

        /*
         * Business Owner's deletion requests.
         */
        if (path.equals(
                "/api/business/deletion-requests/me"
        ) && method == HttpMethod.GET) {

            return OWNER.contains(role);
        }

        /*
         * Pending deletion requests.
         */
        if (path.equals(
                "/api/business/deletion-requests/pending"
        ) && method == HttpMethod.GET) {

            return UNDERWRITING.contains(role);
        }

        /*
         * Business Owner requests deletion.
         */
        if (path.matches(
                "^/api/business/\\d+/deletion-requests$"
        ) && method == HttpMethod.POST) {

            return OWNER.contains(role);
        }

        /*
         * Underwriter/Admin approve deletion.
         */
        if (path.matches(
                "^/api/business/deletion-requests/\\d+/approve$"
        ) && method == HttpMethod.PUT) {

            return UNDERWRITING.contains(role);
        }

        /*
         * Underwriter/Admin reject deletion.
         */
        if (path.matches(
                "^/api/business/deletion-requests/\\d+/reject$"
        ) && method == HttpMethod.PUT) {

            return UNDERWRITING.contains(role);
        }

        /*
         * Business by ID.
         */
        if (isIdPath(
                path,
                "/api/business/"
        )) {

            if (method == HttpMethod.GET) {

                return OWNER.contains(role)
                        || BUSINESS_READ.contains(role);
            }

            if (method == HttpMethod.PUT) {

                return OWNER.contains(role);
            }

            if (method == HttpMethod.DELETE) {

                return ADMIN.contains(role);
            }

            return false;
        }

        // =====================================================
        // POLICIES
        // =====================================================

        /*
         * CREATE POLICY
         *
         * Business Owner only.
         */
        if (path.equals("/api/policies")
                && method == HttpMethod.POST) {

            return OWNER.contains(role);
        }

        /*
         * GET ALL POLICIES
         *
         * Admin / Underwriter / Risk Engineer /
         * Claims Adjuster.
         */
        if (path.equals("/api/policies")
                && method == HttpMethod.GET) {

            return POLICY_READ.contains(role);
        }

        /*
         * GET OWNER POLICIES
         *
         * Business Owner only.
         */
        if (path.equals("/api/policies/owner")
                && method == HttpMethod.GET) {

            return OWNER.contains(role);
        }

        // =====================================================
        // STANDARD / PRELOADED POLICIES
        // =====================================================

        /*
         * THIS FIXES YOUR CURRENT 403.
         *
         * Angular calls:
         *
         * GET /api/policies/standard
         *
         * when the Business Owner opens Create Policy.
         */
        if (path.equals("/api/policies/standard")
                && method == HttpMethod.GET) {

            return STANDARD_POLICY_READ.contains(role);
        }

        /*
         * Individual standard policy template.
         *
         * Angular may call:
         *
         * GET /api/policies/standard/{id}
         */
        if (path.matches(
                "^/api/policies/standard/[^/]+$"
        ) && method == HttpMethod.GET) {

            return STANDARD_POLICY_READ.contains(role);
        }

        /*
         * GET INDIVIDUAL POLICY
         *
         * Example:
         *
         * /api/policies/15
         */
        if (path.matches(
                "^/api/policies/\\d+$"
        )) {

            if (method == HttpMethod.GET) {

                return POLICY_DETAILS.contains(role);
            }

            if (method == HttpMethod.PUT) {

                return OWNER.contains(role);
            }

            return false;
        }

        /*
         * SUBMIT POLICY
         */
        if (path.matches(
                "^/api/policies/\\d+/submit$"
        ) && method == HttpMethod.POST) {

            return OWNER.contains(role);
        }

        /*
         * UNDERWRITER CHANGE POLICY STATUS
         */
        if (path.matches(
                "^/api/policies/\\d+/status$"
        ) && method == HttpMethod.PATCH) {

            return UNDERWRITING.contains(role);
        }

        // =====================================================
        // RISK ASSESSMENTS
        // =====================================================

        /*
         * GET /api/risk/{id}
         */
        if (path.matches(
                "^/api/risk/\\d+$"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        /*
         * GET ALL RISK ASSESSMENTS
         */
        if (path.equals(
                "/api/risk/assessments"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        /*
         * GET SINGLE RISK ASSESSMENT
         */
        if (path.matches(
                "^/api/risk/assessments/\\d+$"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        /*
         * GET RISK ASSESSMENTS FOR BUSINESS
         */
        if (path.matches(
                "^/api/risk/assessments/business/\\d+$"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        /*
         * CREATE RISK ASSESSMENT
         *
         * Risk Engineer only.
         */
        if (path.equals(
                "/api/risk/assessments"
        ) && method == HttpMethod.POST) {

            return role.equals("RISK_ENGINEER");
        }

        // =====================================================
        // RISK SIMULATIONS
        // =====================================================

        /*
         * CREATE RISK SIMULATION
         */
        if (path.equals(
                "/api/risk/simulations"
        ) && method == HttpMethod.POST) {

            return Set.of(
                    "ADMIN",
                    "RISK_ENGINEER"
            ).contains(role);
        }

        /*
         * VIEW RISK SIMULATIONS
         */
        if ((path.equals(
                "/api/risk/simulations"
        ) || path.matches(
                "^/api/risk/simulations/\\d+$"
        )) && method == HttpMethod.GET) {

            return RISK_ANALYST.contains(role);
        }

        // =====================================================
        // RISK MITIGATIONS
        // =====================================================

        if (path.equals(
                "/api/risk/mitigations"
        )) {

            /*
             * Risk Engineer creates mitigation.
             */
            if (method == HttpMethod.POST) {

                return role.equals(
                        "RISK_ENGINEER"
                );
            }

            /*
             * Roles allowed to view mitigations.
             */
            if (method == HttpMethod.GET) {

                return RISK_READ.contains(role);
            }

            return false;
        }

        /*
         * GET mitigations for assessment.
         */
        if (path.matches(
                "^/api/risk/mitigations/assessment/\\d+$"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        /*
         * Update mitigation status.
         */
        if (path.matches(
                "^/api/risk/mitigations/\\d+/status$"
        ) && method == HttpMethod.PATCH) {

            return RISK_MITIGATION_MANAGE.contains(role);
        }

        /*
         * GET individual mitigation.
         */
        if (path.matches(
                "^/api/risk/mitigations/\\d+$"
        ) && method == HttpMethod.GET) {

            return RISK_READ.contains(role);
        }

        // =====================================================
        // CLAIMS
        // =====================================================

        if (path.equals("/api/claims")) {

            /*
             * Business Owner creates claim.
             */
            if (method == HttpMethod.POST) {

                return OWNER.contains(role);
            }

            /*
             * Read claims.
             */
            if (method == HttpMethod.GET) {

                return CLAIMS_READ.contains(role);
            }

            return false;
        }

        /*
         * Business Owner's claims.
         */
        if (path.equals("/api/claims/owner")
                && method == HttpMethod.GET) {

            return OWNER.contains(role);
        }

        /*
         * Individual claim.
         */
        if (path.matches(
                "^/api/claims/\\d+$"
        ) && method == HttpMethod.GET) {

            return CLAIM_DETAILS.contains(role);
        }

        /*
         * Upload claim documents.
         */
        if (path.matches(
                "^/api/claims/\\d+/documents$"
        ) && method == HttpMethod.POST) {

            return CLAIM_DETAILS.contains(role);
        }

        /*
         * Get claim documents.
         */
        if (path.matches(
                "^/api/claims/\\d+/documents$"
        ) && method == HttpMethod.GET) {

            return CLAIM_DETAILS.contains(role);
        }

        /*
         * Get/download specific claim document.
         */
        if (path.matches(
                "^/api/claims/\\d+/documents/[^/]+$"
        ) && method == HttpMethod.GET) {

            return CLAIM_DETAILS.contains(role);
        }

        /*
         * Update claim status.
         */
        if (path.matches(
                "^/api/claims/\\d+/status$"
        ) && method == HttpMethod.PATCH) {

            return CLAIMS_MANAGE.contains(role);
        }

        // =====================================================
        // RECOVERY
        // =====================================================

        /*
         * Create recovery.
         */
        if (path.matches(
                "^/api/claims/\\d+/recovery$"
        ) && method == HttpMethod.POST) {

            return CLAIMS_MANAGE.contains(role);
        }

        /*
         * Get recovery for claim.
         */
        if (path.matches(
                "^/api/claims/\\d+/recovery$"
        ) && method == HttpMethod.GET) {

            return CLAIM_DETAILS.contains(role);
        }

        /*
         * Get all recovery records.
         */
        if (path.equals(
                "/api/claims/recovery"
        ) && method == HttpMethod.GET) {

            return CLAIMS_READ.contains(role);
        }

        /*
         * Update recovery status.
         */
        if (path.matches(
                "^/api/claims/recovery/\\d+/status$"
        ) && method == HttpMethod.PATCH) {

            return CLAIMS_MANAGE.contains(role);
        }

        // =====================================================
        // NOTIFICATIONS
        // =====================================================

        if (path.equals(
                "/api/notifications"
        ) || path.equals(
                "/api/notifications/unread"
        )) {

            return method == HttpMethod.GET;
        }

        /*
         * Mark notification as read.
         */
        if (path.matches(
                "^/api/notifications/\\d+/read$"
        ) && method == HttpMethod.PATCH) {

            return true;
        }

        // =====================================================
        // AUDIT
        // =====================================================

        /*
         * Admin reads audit records.
         */
        if (path.equals("/api/audit")
                && method == HttpMethod.GET) {

            return ADMIN.contains(role);
        }

        /*
         * Any authenticated service can write audit events.
         */
        if (path.equals("/api/audit")
                && method == HttpMethod.POST) {

            return true;
        }

        /*
         * Internal event publishing.
         */
        if (path.equals("/api/events")
                && method == HttpMethod.POST) {

            return true;
        }

        // =====================================================
        // DEFAULT DENY
        // =====================================================

        return false;
    }

    // =========================================================
    // ID PATH HELPER
    // =========================================================

    private boolean isIdPath(
            String path,
            String prefix) {

        if (!path.startsWith(prefix)) {
            return false;
        }

        String id =
                path.substring(prefix.length());

        return ID.matcher(id).matches();
    }

    // =========================================================
    // PUBLIC PATHS
    // =========================================================

    private boolean isPublicPath(
            String path) {

        return path.equals(
                "/api/auth/login"
        )
                || path.equals(
                "/api/auth/register"
        )
                || path.equals(
                "/actuator/health"
        )
                || path.equals(
                "/actuator/info"
        )
                || path.equals(
                "/fallback/identity"
        )
                || path.equals(
                "/fallback/business"
        )
                || path.equals(
                "/fallback/policy"
        )
                || path.equals(
                "/fallback/risk"
        )
                || path.equals(
                "/fallback/claims"
        );
    }

    // =========================================================
    // FORBIDDEN RESPONSE
    // =========================================================

    private Mono<Void> forbidden(
            ServerWebExchange exchange,
            String message) {

        exchange.getResponse()
                .setStatusCode(
                        HttpStatus.FORBIDDEN
                );

        exchange.getResponse()
                .getHeaders()
                .setContentType(
                        MediaType.APPLICATION_JSON
                );

        String path =
                exchange.getRequest()
                        .getURI()
                        .getPath();

        String response = """
                {
                    "status": 403,
                    "error": "Forbidden",
                    "message": "%s",
                    "path": "%s"
                }
                """.formatted(
                escapeJson(message),
                escapeJson(path)
        );

        byte[] bytes =
                response.getBytes(
                        StandardCharsets.UTF_8
                );

        return exchange.getResponse()
                .writeWith(
                        Mono.just(
                                exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(bytes)
                        )
                );
    }

    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String escapeJson(
            String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    // =========================================================
    // FILTER ORDER
    // =========================================================

    @Override
    public int getOrder() {
        return -50;
    }
}