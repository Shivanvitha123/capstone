package org.example.notificationauditservice.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationWebFilter jwtAuthenticationWebFilter;

    public SecurityConfig(
            JwtAuthenticationWebFilter jwtAuthenticationWebFilter
    ) {
        this.jwtAuthenticationWebFilter = jwtAuthenticationWebFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .httpBasic(
                        ServerHttpSecurity.HttpBasicSpec::disable
                )

                .formLogin(
                        ServerHttpSecurity.FormLoginSpec::disable
                )

                .logout(
                        ServerHttpSecurity.LogoutSpec::disable
                )

                .authorizeExchange(exchange -> exchange

                        // Public authentication endpoints
                        .pathMatchers(
                                "/api/auth/login",
                                "/api/auth/register"
                        )
                        .permitAll()

                        // Public actuator endpoints
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        // Authenticated user profile
                        .pathMatchers("/api/auth/me")
                        .authenticated()

                        // Admin-only user management
                        .pathMatchers("/api/auth/users/**")
                        .hasRole("ADMIN")

                        // Admin-only analytics endpoints
                        .pathMatchers("/api/admin/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // All other endpoints require authentication
                        .anyExchange()
                        .authenticated()
                )

                // Add JWT authentication filter
                .addFilterAt(
                        jwtAuthenticationWebFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .build();
    }
}