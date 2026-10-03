package org.example.claimrecoveryservice.security;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationWebFilter implements WebFilter {

    private static final Logger log =
            LoggerFactory.getLogger(JwtAuthenticationWebFilter.class);

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        String token = getToken(exchange);

        if (token == null || token.isBlank()) {
            log.debug("No JWT found for request: {}", path);
            return chain.filter(exchange);
        }

        try {
            if (!jwtService.isValidToken(token)) {
                log.warn("Invalid JWT for request: {}", path);
                return chain.filter(exchange);
            }

            Long userId = jwtService.extractUserId(token);
            String role = normalizeRole(
                    jwtService.extractRole(token)
            );

            if (userId == null || role.isBlank()) {
                log.warn(
                        "JWT missing user ID or role for request: {}",
                        path
                );
                return chain.filter(exchange);
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role
                                    )
                            )
                    );

            log.debug(
                    "Authenticated Claims request: userId={}, role={}, path={}",
                    userId,
                    role,
                    path
            );

            return chain.filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withAuthentication(authentication)
                    );

        } catch (Exception exception) {
            log.error(
                    "JWT processing failed for request: {}",
                    path,
                    exception
            );

            return chain.filter(exchange);
        }
    }

    // =====================================================
    // TOKEN EXTRACTION
    // =====================================================

    private String getToken(ServerWebExchange exchange) {

        // Prefer the HttpOnly cookie.
        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst(ACCESS_TOKEN_COOKIE);

        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // Backward compatibility for Bearer authentication.
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization != null
                && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        return null;
    }

    // =====================================================
    // ROLE NORMALIZATION
    // =====================================================

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
}