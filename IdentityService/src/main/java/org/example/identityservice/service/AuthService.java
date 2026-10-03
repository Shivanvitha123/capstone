package org.example.identityservice.service;

import org.example.identityservice.dto.AuthResponse;
import org.example.identityservice.dto.AuthSession;
import org.example.identityservice.dto.LoginRequest;
import org.example.identityservice.dto.RegisterRequest;
import org.example.identityservice.entity.User;
import org.example.identityservice.exception.InvalidCredentialsException;
import org.example.identityservice.exception.UserAlreadyExistsException;
import org.example.identityservice.model.Role;
import org.example.identityservice.repository.UserRepository;
import org.example.identityservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<AuthSession> register(RegisterRequest request) {

        String email = normalizeEmail(request.email());

        return userRepository.existsByEmail(email)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(
                                new UserAlreadyExistsException(
                                        "User with email already exists: " + email
                                )
                        );
                    }

                    LocalDateTime now = LocalDateTime.now();

                    User user = User.builder()
                            .name(request.name().trim())
                            .email(email)
                            .password(
                                    passwordEncoder.encode(
                                            request.password()
                                    )
                            )
                            .role(Role.BUSINESS_OWNER)
                            .active(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return userRepository.save(user);
                })
                .map(this::createAuthSession);
    }

    public Mono<AuthSession> login(LoginRequest request) {

        String email = normalizeEmail(request.email());

        return userRepository.findByEmail(email)
                .switchIfEmpty(
                        Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        )
                )
                .flatMap(user -> {

                    if (!Boolean.TRUE.equals(user.getActive())) {
                        return Mono.error(
                                new InvalidCredentialsException(
                                        "User account is inactive"
                                )
                        );
                    }

                    if (!passwordEncoder.matches(
                            request.password(),
                            user.getPassword()
                    )) {
                        return Mono.error(
                                new InvalidCredentialsException(
                                        "Invalid email or password"
                                )
                        );
                    }

                    return Mono.just(createAuthSession(user));
                });
    }

    private AuthSession createAuthSession(User user) {

        String token = jwtService.generateToken(user);

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthSession(token, response);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}