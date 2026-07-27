package com.vyay.core.services.auth;

import com.vyay.core.common.utils.EmailUtils;
import com.vyay.core.dto.requests.auth.BasePasswordRegisterRequest;
import com.vyay.core.dto.response.Auth.RegisterResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.enums.PasswordRegistrationVerificationChannel;
import com.vyay.core.enums.RegistrationNextStep;
import com.vyay.core.exception.auth.PasswordPolicyViolationException;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.security.JwtService;
import com.vyay.core.security.PasswordPolicy;
import com.vyay.core.services.generateOtp.OtpChallenge;
import com.vyay.core.services.generateOtp.OtpExecutorService;
import com.vyay.core.services.notification.NotificationCommand;
import com.vyay.core.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PasswordRegistrationService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final OtpExecutorService otpExecutorService;
    private final UserRepository userRepository;
    private final List<PasswordUserRegistrationHandler<? extends BasePasswordRegisterRequest>> handlers;

    private final boolean skipEmailVerification;

    public PasswordRegistrationService(PasswordEncoder passwordEncoder,
                                       PasswordPolicy passwordPolicy,
                                       JwtService jwtService,
                                       NotificationService notificationService,
                                       OtpExecutorService otpExecutorService,
                                       UserRepository userRepository,
                                       List<PasswordUserRegistrationHandler<? extends BasePasswordRegisterRequest>> handlers,
                                       @Value("${app.auth.skip-email-verification:false}") boolean skipEmailVerification) {
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.otpExecutorService = otpExecutorService;
        this.userRepository = userRepository;
        this.handlers = handlers;
        this.skipEmailVerification = skipEmailVerification;
    }

    @SuppressWarnings("unchecked")
    public <T extends BasePasswordRegisterRequest> RegisterResponseDTO register(T request) {
        PasswordUserRegistrationHandler<T> handler = (PasswordUserRegistrationHandler<T>) handlers.stream()
                .filter(h -> h.supports().isAssignableFrom(request.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No registration handler for " + request.getClass().getSimpleName()));

        List<String> violations = passwordPolicy.validate(request.getPassword());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(String.join(", ", violations));
        }

        String email = EmailUtils.normalize(request.getEmail());

        String hash = passwordEncoder.encode(request.getPassword());
        User user = handler.createUser(request, hash);

        if (skipEmailVerification) {
            user.setEmailVerified(true);
            userRepository.save(user);
            return buildSkipVerificationResponse(email);
        }

        PasswordRegistrationVerificationChannel channel = request.getVerificationType();

        return switch (channel) {
            case EMAIL_OTP -> handleEmailOtp(user, email);
            case EMAIL_LINK -> handleEmailLink(user, email);
        };
    }

    private RegisterResponseDTO handleEmailOtp(User user, String email) {
        NotificationCommand command = NotificationCommand.builder()
                .type("EMAIL_VERIFICATION_OTP")
                .channel("EMAIL")
                .recipient(email)
                .variables(Map.of("firstName", user.getFirstName()))
                .build();

        OtpChallenge challenge = otpExecutorService.issue(user.getId(), List.of(command));

        return RegisterResponseDTO.builder()
                .email(email)
                .verificationRequired(true)
                .verificationType(PasswordRegistrationVerificationChannel.EMAIL_OTP)
                .nextStep(RegistrationNextStep.VERIFY_OTP)
                .verificationId(challenge.getVerificationId())
                .expiresAt(challenge.getExpiresAt())
                .resendAvailableAt(challenge.getResendAvailableAt())
                .message("Verification code sent. Please check your inbox.")
                .build();
    }

    private RegisterResponseDTO handleEmailLink(User user, String email) {
        String verificationToken = jwtService.generateEmailVerificationToken(user.getId());

        notificationService.send(
                "EMAIL_VERIFICATION",
                "EMAIL",
                email,
                Map.of(
                        "firstName", user.getFirstName(),
                        "link", verificationToken
                )
        );

        return RegisterResponseDTO.builder()
                .email(email)
                .verificationRequired(true)
                .verificationType(PasswordRegistrationVerificationChannel.EMAIL_LINK)
                .nextStep(RegistrationNextStep.VERIFY_LINK)
                .message("Verification email sent. Please check your inbox.")
                .build();
    }

    private RegisterResponseDTO buildSkipVerificationResponse(String email) {
        return RegisterResponseDTO.builder()
                .email(email)
                .verificationRequired(false)
                .nextStep(RegistrationNextStep.LOGIN)
                .message("User registered and auto-verified (dev mode).")
                .build();
    }
}