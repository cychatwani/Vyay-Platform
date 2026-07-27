package com.vyay.core.services.auth;

import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.enums.TokenType;
import com.vyay.core.security.JwtService;
import com.vyay.core.services.user.UserService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmailVerificationService {

    private final JwtService jwtService;
    private final UserService userService;
    private final SessionIssuer sessionIssuer;

    public EmailVerificationService(JwtService jwtService,
                                    UserService userService,
                                    SessionIssuer sessionIssuer) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.sessionIssuer = sessionIssuer;
    }

    public void verify(String token) {
        resolveAndVerify(token);
    }

    public AuthResponseDTO verifyAndLogin(String token) {
        User user = resolveAndVerify(token);
        return sessionIssuer.issueSession(user, AuthProvider.PASSWORD, false);
    }

    private User resolveAndVerify(String token) {
        UUID userId = jwtService.getUserIdFromToken(token, TokenType.EMAIL_VERIFICATION.getValue());

        User user = userService.getById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid verification token"));

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            userService.save(user);
        }

        return user;
    }
}
