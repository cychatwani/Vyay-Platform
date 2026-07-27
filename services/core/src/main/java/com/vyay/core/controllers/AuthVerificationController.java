package com.vyay.core.controllers;

import com.vyay.core.common.utils.EmailUtils;
import com.vyay.core.dto.requests.verification.ResendOtpRequestDTO;
import com.vyay.core.dto.requests.verification.VerifyEmailOtpRequestDTO;
import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.services.auth.SessionIssuer;
import com.vyay.core.services.generateOtp.OtpChallenge;
import com.vyay.core.services.generateOtp.OtpExecutorService;
import com.vyay.core.services.notification.NotificationCommand;
import com.vyay.core.services.user.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/verification/auth")
public class AuthVerificationController {

    private final OtpExecutorService otpExecutorService;
    private final UserService userService;
    private final SessionIssuer sessionIssuer;

    public AuthVerificationController(OtpExecutorService otpExecutorService,
                                      UserService userService,
                                      SessionIssuer sessionIssuer) {
        this.otpExecutorService = otpExecutorService;
        this.userService = userService;
        this.sessionIssuer = sessionIssuer;
    }

    @PostMapping("/verify-email-otp")
    public ResponseEntity<ApiResponse<Void>> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequestDTO requestBody) {
        resolveAndVerifyOtp(requestBody.getVerificationId(), requestBody.getOtp());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(null, "Email verified successfully."));
    }

    @PostMapping("/verify-email-otp-and-login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> verifyEmailOtpAndLogin(
            @Valid @RequestBody VerifyEmailOtpRequestDTO requestBody) {
        User user = resolveAndVerifyOtp(requestBody.getVerificationId(), requestBody.getOtp());
        AuthResponseDTO payload = sessionIssuer.issueSession(user, AuthProvider.PASSWORD, false);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(payload));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OtpChallenge>> resendOtp(
            @Valid @RequestBody ResendOtpRequestDTO requestBody) {
        String email = EmailUtils.normalize(requestBody.getEmail());
        User user = userService.getByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid request"));

        if (user.isEmailVerified()) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Email is already verified."));
        }

        NotificationCommand command = NotificationCommand.builder()
                .type("EMAIL_VERIFICATION_OTP")
                .channel("EMAIL")
                .recipient(email)
                .variables(Map.of("firstName", user.getFirstName()))
                .build();

        OtpChallenge challenge = otpExecutorService.resend(user.getId(), List.of(command));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(challenge));
    }

    private User resolveAndVerifyOtp(String verificationId, String submittedOtp) {
        UUID userId = otpExecutorService.verify(verificationId, submittedOtp);
        User user = userService.getById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid verification"));
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userService.save(user);
        }
        return user;
    }
}