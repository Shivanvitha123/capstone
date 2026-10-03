package org.example.identityservice.controller;

import org.example.identityservice.dto.AuthResponse;
import org.example.identityservice.dto.AuthSession;
import org.example.identityservice.dto.LoginRequest;
import org.example.identityservice.dto.RegisterRequest;
import org.example.identityservice.dto.UserResponse;
import org.example.identityservice.model.Role;
import org.example.identityservice.service.AuthService;
import org.example.identityservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AuthService authService;
    private UserService userService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        userService = mock(UserService.class);

        controller = new AuthController(
                authService,
                userService,
                false
        );

        ReflectionTestUtils.setField(controller, "jwtExpiration", 3600000L);
    }

    private AuthResponse authResponse() {
        return new AuthResponse(
                1L,
                "Test User",
                "test@example.com",
                Role.BUSINESS_OWNER
        );
    }

    private AuthSession authSession() {
        return new AuthSession(
                "test-token",
                authResponse()
        );
    }

    private UserResponse userResponse(Long id) {
        LocalDateTime now = LocalDateTime.now();

        return new UserResponse(
                id,
                "Test User",
                "test@example.com",
                Role.BUSINESS_OWNER,
                true,
                now,
                now
        );
    }

    @Test
    void shouldRegisterUser() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        when(authService.register(request))
                .thenReturn(Mono.just(authSession()));

        StepVerifier.create(controller.register(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.CREATED, response.getStatusCode());

                    AuthResponse body = response.getBody();
                    assertNotNull(body);
                    assertEquals(1L, body.userId());
                    assertEquals("Test User", body.name());
                    assertEquals("test@example.com", body.email());
                    assertEquals(Role.BUSINESS_OWNER, body.role());

                    String cookie = response.getHeaders()
                            .getFirst(HttpHeaders.SET_COOKIE);

                    assertNotNull(cookie);
                    assertTrue(cookie.contains("ACCESS_TOKEN=test-token"));
                    assertTrue(cookie.contains("HttpOnly"));
                    assertTrue(cookie.contains("Path=/"));
                    assertTrue(cookie.contains("SameSite=Lax"));
                    assertTrue(cookie.contains("Max-Age=3600"));
                })
                .verifyComplete();

        verify(authService).register(request);
    }

    @Test
    void shouldLoginUser() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        when(authService.login(request))
                .thenReturn(Mono.just(authSession()));

        StepVerifier.create(controller.login(request))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());

                    AuthResponse body = response.getBody();
                    assertNotNull(body);
                    assertEquals(1L, body.userId());
                    assertEquals("Test User", body.name());
                    assertEquals(Role.BUSINESS_OWNER, body.role());

                    String cookie = response.getHeaders()
                            .getFirst(HttpHeaders.SET_COOKIE);

                    assertNotNull(cookie);
                    assertTrue(cookie.contains("ACCESS_TOKEN=test-token"));
                    assertTrue(cookie.contains("HttpOnly"));
                    assertTrue(cookie.contains("Path=/"));
                    assertTrue(cookie.contains("SameSite=Lax"));
                    assertTrue(cookie.contains("Max-Age=3600"));
                })
                .verifyComplete();

        verify(authService).login(request);
    }

    @Test
    void shouldLogoutUser() {
        StepVerifier.create(controller.logout())
                .assertNext(response -> {
                    assertEquals(
                            HttpStatus.NO_CONTENT,
                            response.getStatusCode()
                    );

                    String cookie = response.getHeaders()
                            .getFirst(HttpHeaders.SET_COOKIE);

                    assertNotNull(cookie);
                    assertTrue(cookie.contains("ACCESS_TOKEN="));
                    assertTrue(cookie.contains("HttpOnly"));
                    assertTrue(cookie.contains("Path=/"));
                    assertTrue(cookie.contains("SameSite=Lax"));
                    assertTrue(cookie.contains("Max-Age=0"));
                })
                .verifyComplete();
    }

    @Test
    void shouldGetCurrentUser() {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken("1", null);

        UserResponse expected = userResponse(1L);

        when(userService.getUserById(1L))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(controller.getCurrentUser(authentication))
                .assertNext(response -> {
                    assertEquals(1L, response.id());
                    assertEquals("Test User", response.name());
                    assertEquals("test@example.com", response.email());
                })
                .verifyComplete();

        verify(userService).getUserById(1L);
    }

    @Test
    void shouldGetAllUsers() {
        UserResponse first = userResponse(1L);
        UserResponse second = userResponse(2L);

        when(userService.getAllUsers())
                .thenReturn(Flux.just(first, second));

        StepVerifier.create(controller.getAllUsers())
                .expectNext(first)
                .expectNext(second)
                .verifyComplete();

        verify(userService).getAllUsers();
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoUsers() {
        when(userService.getAllUsers())
                .thenReturn(Flux.empty());

        StepVerifier.create(controller.getAllUsers())
                .verifyComplete();

        verify(userService).getAllUsers();
    }

    @Test
    void shouldGetUserById() {
        UserResponse expected = userResponse(10L);

        when(userService.getUserById(10L))
                .thenReturn(Mono.just(expected));

        StepVerifier.create(controller.getUserById(10L))
                .assertNext(response -> {
                    assertEquals(10L, response.id());
                    assertEquals("Test User", response.name());
                })
                .verifyComplete();

        verify(userService).getUserById(10L);
    }

    @Test
    void shouldPropagateRegistrationError() {
        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        when(authService.register(request))
                .thenReturn(Mono.error(
                        new RuntimeException("Registration failed")
                ));

        StepVerifier.create(controller.register(request))
                .expectErrorMessage("Registration failed")
                .verify();

        verify(authService).register(request);
    }

    @Test
    void shouldPropagateLoginError() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        when(authService.login(request))
                .thenReturn(Mono.error(
                        new RuntimeException("Login failed")
                ));

        StepVerifier.create(controller.login(request))
                .expectErrorMessage("Login failed")
                .verify();

        verify(authService).login(request);
    }
}