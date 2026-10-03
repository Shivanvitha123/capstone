package org.example.businessservice.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtAuthenticationWebFilter implements WebFilter {

    private final JwtService jwtService;

    public JwtAuthenticationWebFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            WebFilterChain chain
    ) {
        String token = null;

        // First, read the HttpOnly cookie.
        HttpCookie cookie =
                exchange.getRequest()
                        .getCookies()
                        .getFirst("ACCESS_TOKEN");

        if (cookie != null && !cookie.getValue().isBlank()) {
            token = cookie.getValue();
        }

        // Backward compatibility with Bearer authentication.
        if (token == null) {
            String authorization =
                    exchange.getRequest()
                            .getHeaders()
                            .getFirst(HttpHeaders.AUTHORIZATION);

            if (authorization != null
                    && authorization.startsWith("Bearer ")) {
                token = authorization.substring(7);
            }
        }

        // Continue without authentication if no token exists.
        if (token == null || token.isBlank()) {
            return chain.filter(exchange);
        }

        if (!jwtService.isValid(token)) {
            return chain.filter(exchange);
        }

        try {
            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            if (userId == null || role == null || role.isBlank()) {
                return chain.filter(exchange);
            }

            // Prevent ROLE_ROLE_BUSINESS_OWNER.
            String normalizedRole = role.startsWith("ROLE_")
                    ? role
                    : "ROLE_" + role;

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(normalizedRole)
                            )
                    );

            SecurityContext securityContext =
                    new SecurityContextImpl(authentication);

            return chain.filter(exchange)
                    .contextWrite(
                            ReactiveSecurityContextHolder
                                    .withSecurityContext(
                                            Mono.just(securityContext)
                                    )
                    );

        } catch (Exception ex) {
            return chain.filter(exchange);
        }
    }
}