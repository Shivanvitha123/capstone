package org.example.underwritingpolicyservice.security;

import lombok.RequiredArgsConstructor;
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

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    private final JwtService jwtService;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain) {

        String token = extractToken(exchange);

        // No token: continue and let Spring Security
        // enforce authentication.
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        try {
            if (!jwtService.isValidToken(token)) {
                return chain.filter(exchange);
            }

            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            if (userId == null || role == null || role.isBlank()) {
                return chain.filter(exchange);
            }

            // Avoid duplicate ROLE_ prefixes.
            String normalizedRole = role.trim().toUpperCase();

            if (normalizedRole.startsWith("ROLE_")) {
                normalizedRole =
                        normalizedRole.substring("ROLE_".length());
            }

            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + normalizedRole
                                    )
                            )
                    );

            return chain
                    .filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withAuthentication(authentication)
                    );

        } catch (Exception ex) {
            // Invalid tokens are treated as unauthenticated.
            return chain.filter(exchange);
        }
    }

    private String extractToken(ServerWebExchange exchange) {

        // 1. Read the HttpOnly ACCESS_TOKEN cookie.
        HttpCookie cookie = exchange.getRequest()
                .getCookies()
                .getFirst(ACCESS_TOKEN_COOKIE);

        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        // 2. Fall back to the Authorization header.
        String authorizationHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null
                && authorizationHeader.startsWith("Bearer ")) {

            return authorizationHeader.substring(7).trim();
        }

        return null;
    }
}