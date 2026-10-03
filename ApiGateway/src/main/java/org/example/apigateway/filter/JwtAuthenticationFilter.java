package org.example.apigateway.filter;

import org.example.apigateway.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    private static final String USER_ID_HEADER = "X-User-Id";

    private static final String USER_ROLE_HEADER = "X-User-Role";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/logout",
            "/actuator/health",
            "/actuator/info",
            "/fallback"
    );

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        // Allow CORS preflight requests.
        if (exchange.getRequest().getMethod() == null
                || "OPTIONS".equalsIgnoreCase(
                exchange.getRequest().getMethod().name())) {
            return chain.filter(exchange);
        }

        // Allow public endpoints.
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT from cookie or Authorization header.
        String token = extractToken(exchange);

        if (token == null || token.isBlank()) {
            log.warn(
                    "Missing authentication token for request: {}",
                    path
            );

            return unauthorized(exchange);
        }

        try {
            // Validate JWT.
            if (!jwtService.isValid(token)) {
                log.warn(
                        "Invalid JWT for request: {}",
                        path
                );

                return unauthorized(exchange);
            }

            // Extract authenticated user information.
            String userId = jwtService.extractUserId(token);

            String role = normalizeRole(
                    jwtService.extractRole(token)
            );

            // A valid user ID is mandatory.
            // A missing role is allowed, but no role header
            // will be forwarded in that case.
            if (userId == null || userId.isBlank()) {
                log.warn(
                        "JWT missing user ID for request: {}",
                        path
                );

                return unauthorized(exchange);
            }

            log.debug(
                    "Authenticated request. userId={}, role={}, path={}",
                    userId,
                    role,
                    path
            );

            /*
             * Remove client-supplied identity headers.
             * Add trusted identity headers and forward
             * the validated JWT to downstream services.
             */
            ServerHttpRequest mutatedRequest =
                    exchange.getRequest()
                            .mutate()
                            .headers(headers -> {

                                // Remove untrusted client headers.
                                headers.remove(USER_ID_HEADER);
                                headers.remove(USER_ROLE_HEADER);

                                // Add authenticated user ID.
                                headers.set(
                                        USER_ID_HEADER,
                                        userId
                                );

                                // Add role only if present in JWT.
                                if (role != null && !role.isBlank()) {
                                    headers.set(
                                            USER_ROLE_HEADER,
                                            role
                                    );
                                }

                                // Forward the validated JWT.
                                headers.set(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                );
                            })
                            .build();

            ServerWebExchange mutatedExchange =
                    exchange.mutate()
                            .request(mutatedRequest)
                            .build();

            return chain.filter(mutatedExchange);

        } catch (Exception exception) {

            log.error(
                    "JWT processing failed for path {}",
                    path,
                    exception
            );

            return unauthorized(exchange);
        }
    }

    /**
     * Extracts the JWT from the HttpOnly cookie.
     * Supports Bearer tokens for backward compatibility.
     */
    private String extractToken(ServerWebExchange exchange) {

        // Prefer the HttpOnly cookie.
        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst(COOKIE_NAME);

        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // Fallback to the Authorization header.
        String authorizationHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            return authorizationHeader
                    .substring(7)
                    .trim();
        }

        return null;
    }

    /**
     * Determines whether the requested path is public.
     */
    private boolean isPublicPath(String path) {

        return PUBLIC_PATHS.stream()
                .anyMatch(publicPath ->
                        path.equals(publicPath)
                                || path.startsWith(publicPath + "/")
                );
    }

    /**
     * Normalizes role names.
     *
     * Examples:
     * ROLE_ADMIN       -> ADMIN
     * BUSINESS_OWNER   -> BUSINESS_OWNER
     * claims-adjuster  -> CLAIMS_ADJUSTER
     */
    private String normalizeRole(String role) {

        if (role == null || role.isBlank()) {
            return "";
        }

        String normalized = role.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }

        return normalized;
    }

    /**
     * Returns a JSON 401 response.
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        exchange.getResponse()
                .getHeaders()
                .setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest()
                .getURI()
                .getPath()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        String response = """
                {
                    "status": 401,
                    "error": "Unauthorized",
                    "message": "Valid authentication token is required",
                    "path": "%s"
                }
                """.formatted(path);

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse()
                .writeWith(
                        Mono.just(
                                exchange.getResponse()
                                        .bufferFactory()
                                        .wrap(bytes)
                        )
                );
    }

    @Override
    public int getOrder() {
        return -100;
    }
}