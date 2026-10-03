package org.example.identityservice.controller;

import jakarta.validation.Valid;
import org.example.identityservice.dto.AuthResponse;
import org.example.identityservice.dto.AuthSession;
import org.example.identityservice.dto.LoginRequest;
import org.example.identityservice.dto.RegisterRequest;
import org.example.identityservice.dto.UserResponse;
import org.example.identityservice.service.AuthService;
import org.example.identityservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_NAME = "ACCESS_TOKEN";

    private final AuthService authService;
    private final UserService userService;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            UserService userService,
            @Value("${app.cookie.secure:false}") boolean secureCookie
    ) {
        this.authService = authService;
        this.userService = userService;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(request)
                .map(session -> createAuthResponse(
                        session,
                        HttpStatus.CREATED
                ));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request)
                .map(session -> createAuthResponse(
                        session,
                        HttpStatus.OK
                ));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout() {

        ResponseCookie cookie = ResponseCookie
                .from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        return Mono.just(
                ResponseEntity.noContent()
                        .header(HttpHeaders.SET_COOKIE, cookie.toString())
                        .build()
        );
    }

    @GetMapping("/me")
    public Mono<UserResponse> getCurrentUser(
            Authentication authentication
    ) {
        Long userId = Long.valueOf(authentication.getName());
        return userService.getUserById(userId);
    }

    @GetMapping("/users")
    public Flux<UserResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/users/{id}")
    public Mono<UserResponse> getUserById(
            @PathVariable Long id
    ) {
        return userService.getUserById(id);
    }

    private ResponseEntity<AuthResponse> createAuthResponse(
            AuthSession session,
            HttpStatus status
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(COOKIE_NAME, session.token())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(
                        authServiceTokenExpiration(session)
                ))
                .build();

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(session.user());
    }

    private long authServiceTokenExpiration(AuthSession session) {
        // The JWT's configured expiration is used for the cookie.
        return jwtExpiration;
    }

    @Value("${jwt.expiration}")
    private long jwtExpiration;
}