package org.example.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                // Disable CSRF because authentication is handled
                // through JWT by the custom Gateway filters.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Disable HTTP Basic authentication.
                .httpBasic(
                        ServerHttpSecurity.HttpBasicSpec::disable
                )

                // Disable form-based login.
                .formLogin(
                        ServerHttpSecurity.FormLoginSpec::disable
                )

                // Disable Spring Security's default logout.
                .logout(
                        ServerHttpSecurity.LogoutSpec::disable
                )

                /*
                 * Gateway authentication and authorization:
                 *
                 * 1. JwtAuthenticationFilter validates the JWT.
                 * 2. RbacAuthorizationFilter checks permissions.
                 *
                 * These are custom Gateway GlobalFilters.
                 * They must enforce authentication and authorization
                 * independently of the rules configured below.
                 */
                .authorizeExchange(exchange -> exchange

                        // Public authentication endpoints
                        .pathMatchers(
                                "/api/auth/login",
                                "/api/auth/register"
                        ).permitAll()

                        // Public health and information endpoints
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        /*
                         * All other requests continue through the
                         * custom JWT and RBAC Gateway filters.
                         *
                         * Do not add permitAll() exceptions for
                         * protected endpoints in the custom filters.
                         */
                        .anyExchange().permitAll()
                )

                .build();
    }
}