package org.example.claimrecoveryservice.controller;

import org.example.claimrecoveryservice.dto.ClaimResponse;
import org.example.claimrecoveryservice.dto.CreateClaimRequest;
import org.example.claimrecoveryservice.dto.UpdateClaimStatusRequest;
import org.example.claimrecoveryservice.model.ClaimStatus;
import org.example.claimrecoveryservice.model.ClaimType;
import org.example.claimrecoveryservice.service.ClaimFileService;
import org.example.claimrecoveryservice.service.ClaimService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService service;

    @Mock
    private ClaimFileService claimFileService;

    @InjectMocks
    private ClaimController controller;

    private UsernamePasswordAuthenticationToken ownerAuth;
    private ClaimResponse response;

    @BeforeEach
    void setUp() {

        ownerAuth = new UsernamePasswordAuthenticationToken(
                "100",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_BUSINESS_OWNER")
                )
        );

        response = new ClaimResponse(
                1L,
                10L,
                20L,
                100L,
                "CLM-001",
                ClaimType.PROPERTY_DAMAGE,
                LocalDate.now(),
                LocalDate.now(),
                new BigDecimal("5000"),
                new BigDecimal("4000"),
                "Property damage",
                ClaimStatus.SUBMITTED,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    // =====================================================
    // CREATE CLAIM
    // =====================================================

    @Test
    void createClaimReturnsCreated() {

        CreateClaimRequest request = mock(CreateClaimRequest.class);

        when(service.createClaim(
                request,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        StepVerifier.create(
                        controller.createClaim(request, ownerAuth)
                )
                .assertNext(result -> {
                    assertEquals(
                            HttpStatus.CREATED,
                            result.getStatusCode()
                    );

                    assertEquals(response, result.getBody());
                })
                .verifyComplete();

        verify(service).createClaim(
                request,
                100L,
                "BUSINESS_OWNER"
        );
    }

    // =====================================================
    // GET ALL CLAIMS
    // BUSINESS OWNER: OWN CLAIMS
    // =====================================================

    @Test
    void getAllClaimsReturnsOk() {

        when(service.getOwnerClaims(
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Flux.just(response));

        StepVerifier.create(
                        controller.getAllClaims(ownerAuth)
                )
                .assertNext(result -> {

                    assertEquals(
                            HttpStatus.OK,
                            result.getStatusCode()
                    );

                    assertNotNull(result.getBody());

                    StepVerifier.create(result.getBody())
                            .expectNext(response)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).getOwnerClaims(
                100L,
                "BUSINESS_OWNER"
        );

        verify(service, never()).getAllClaims(
                any(),
                any()
        );
    }

    // =====================================================
    // GET OWNER CLAIMS
    // =====================================================

    @Test
    void getOwnerClaimsReturnsOk() {

        when(service.getOwnerClaims(
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Flux.just(response));

        StepVerifier.create(
                        controller.getOwnerClaims(ownerAuth)
                )
                .assertNext(result -> {

                    assertEquals(
                            HttpStatus.OK,
                            result.getStatusCode()
                    );

                    assertNotNull(result.getBody());

                    StepVerifier.create(result.getBody())
                            .expectNext(response)
                            .verifyComplete();
                })
                .verifyComplete();

        verify(service).getOwnerClaims(
                100L,
                "BUSINESS_OWNER"
        );
    }

    // =====================================================
    // GET CLAIM BY ID
    // =====================================================

    @Test
    void getClaimReturnsClaim() {

        when(service.getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        StepVerifier.create(
                        controller.getClaim(1L, ownerAuth)
                )
                .expectNext(response)
                .verifyComplete();

        verify(service).getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        );
    }

    // =====================================================
    // UPDATE CLAIM STATUS
    // =====================================================

    @Test
    void updateClaimStatusReturnsClaim() {

        UpdateClaimStatusRequest request =
                mock(UpdateClaimStatusRequest.class);

        when(service.updateClaimStatus(
                1L,
                request,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        StepVerifier.create(
                        controller.updateClaimStatus(
                                1L,
                                request,
                                ownerAuth
                        )
                )
                .expectNext(response)
                .verifyComplete();

        verify(service).updateClaimStatus(
                1L,
                request,
                100L,
                "BUSINESS_OWNER"
        );
    }

    // =====================================================
    // UPLOAD CLAIM DOCUMENT
    // =====================================================

    @Test
    void uploadClaimDocumentReturnsCreated() {

        FilePart file = mock(FilePart.class);

        when(service.getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        when(claimFileService.uploadFile(
                "1",
                file
        )).thenReturn(Mono.just("document.pdf"));

        StepVerifier.create(
                        controller.uploadClaimDocument(
                                1L,
                                file,
                                ownerAuth
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            HttpStatus.CREATED,
                            result.getStatusCode()
                    );

                    assertEquals(
                            "document.pdf",
                            result.getBody()
                    );
                })
                .verifyComplete();

        verify(service).getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        );

        verify(claimFileService).uploadFile(
                "1",
                file
        );
    }

    // =====================================================
    // GET CLAIM DOCUMENTS
    // =====================================================

    @Test
    void getClaimDocumentsReturnsFiles() {

        List<String> files = List.of(
                "document.pdf",
                "image.png"
        );

        when(service.getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        when(claimFileService.listFiles(
                "1"
        )).thenReturn(Mono.just(files));

        StepVerifier.create(
                        controller.getClaimDocuments(
                                1L,
                                ownerAuth
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            HttpStatus.OK,
                            result.getStatusCode()
                    );

                    assertEquals(
                            files,
                            result.getBody()
                    );
                })
                .verifyComplete();

        verify(service).getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        );

        verify(claimFileService).listFiles("1");
    }

    // =====================================================
    // DOWNLOAD CLAIM DOCUMENT
    // =====================================================

    @Test
    void downloadClaimDocumentReturnsResource() {

        Path path = Path.of("document.pdf");

        when(service.getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        )).thenReturn(Mono.just(response));

        when(claimFileService.getFile(
                "1",
                "document.pdf"
        )).thenReturn(Mono.just(path));

        StepVerifier.create(
                        controller.downloadClaimDocument(
                                1L,
                                "document.pdf",
                                ownerAuth
                        )
                )
                .assertNext(result -> {

                    assertEquals(
                            HttpStatus.OK,
                            result.getStatusCode()
                    );

                    assertNotNull(result.getBody());

                    assertTrue(
                            result.getBody() instanceof Resource
                    );

                    assertEquals(
                            "attachment; filename=\"document.pdf\"",
                            result.getHeaders().getFirst(
                                    "Content-Disposition"
                            )
                    );
                })
                .verifyComplete();

        verify(service).getClaim(
                1L,
                100L,
                "BUSINESS_OWNER"
        );

        verify(claimFileService).getFile(
                "1",
                "document.pdf"
        );
    }

    // =====================================================
    // NULL AUTHENTICATION
    // =====================================================

    @Test
    void getUserIdReturnsNullForNullAuthentication() {

        StepVerifier.create(
                        controller.getAllClaims(null)
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verifyNoInteractions(service);
    }

    // =====================================================
    // INVALID USER ID
    // =====================================================

    @Test
    void getUserIdReturnsNullForInvalidName() {

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "invalid",
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_ADMIN")
                        )
                );

        StepVerifier.create(
                        controller.getAllClaims(authentication)
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verifyNoInteractions(service);
    }

    // =====================================================
    // MISSING AUTHENTICATION / ROLE
    // =====================================================

    @Test
    void getRoleReturnsNullForMissingAuthentication() {

        StepVerifier.create(
                        controller.getOwnerClaims(null)
                )
                .expectError(AccessDeniedException.class)
                .verify();

        verifyNoInteractions(service);
    }
}