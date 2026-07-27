package com.vyay.core.controllers;

import com.vyay.core.dto.requests.auth.GoogleAuthRequestDTO;
import com.vyay.core.dto.requests.auth.PasswordAuthRequestDTO;
import com.vyay.core.dto.requests.auth.PasswordRegisterRequestDTO;
import com.vyay.core.dto.requests.auth.RefreshTokenAuthRequestDto;
import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.dto.response.Auth.RegisterResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.services.auth.EmailVerificationService;
import com.vyay.core.services.auth.GoogleAuthenticationService;
import com.vyay.core.services.auth.PasswordAuthenticationService;
import com.vyay.core.services.auth.PasswordRegistrationService;
import com.vyay.core.services.auth.RefreshAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleAuthenticationService googleAuthenticationService;
    private final RefreshAuthenticationService refreshAuthenticationService;
    private final PasswordRegistrationService passwordRegistrationService;
    private final PasswordAuthenticationService passwordAuthenticationService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(GoogleAuthenticationService googleAuthenticationService,
                          RefreshAuthenticationService refreshAuthenticationService,
                          PasswordRegistrationService passwordRegistrationService,
                          PasswordAuthenticationService passwordAuthenticationService,
                          EmailVerificationService emailVerificationService) {
        this.googleAuthenticationService = googleAuthenticationService;
        this.refreshAuthenticationService = refreshAuthenticationService;
        this.passwordRegistrationService = passwordRegistrationService;
        this.passwordAuthenticationService = passwordAuthenticationService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseDTO>> register(
            @Valid @RequestBody PasswordRegisterRequestDTO requestBody) {
        RegisterResponseDTO payload = passwordRegistrationService.register(requestBody);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(payload));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody PasswordAuthRequestDTO requestBody) {
        AuthResponseDTO payload = passwordAuthenticationService.authenticate(requestBody);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(payload));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam("token") String token) {
        emailVerificationService.verify(token);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Email verified successfully."));
    }

    @GetMapping("/verify-and-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verifyAndLogin(
            @RequestParam("token") String token) {
        AuthResponseDTO payload = emailVerificationService.verifyAndLogin(token);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(payload));
    }

    @PostMapping("/refresh")
    ResponseEntity<ApiResponse<Object>> authWithRefreshToken(
            @Valid @RequestBody RefreshTokenAuthRequestDto requestBody) {
        try {
            AuthResponseDTO payload = refreshAuthenticationService.authenticate(requestBody);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success(payload));
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(ex.getMessage(), "REFRESH_AUTHENTICATION_FAILED"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<Object>> authWithGoogle(
            @Valid @RequestBody GoogleAuthRequestDTO requestBody) {
        try {
            Object payload = googleAuthenticationService.authenticate(requestBody);
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success(payload));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage(), "INVALID_TOKEN"));
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Token verification failed", "TOKEN_VERIFICATION_ERROR"));
        }
    }
}
