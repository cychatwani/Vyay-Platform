package com.vyay.core.services.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.vyay.core.dto.requests.auth.GoogleAuthRequestDTO;
import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.services.user.UserService;
import org.springframework.stereotype.Component;

@Component
public final class GoogleAuthenticationService implements AuthenticationProvider<GoogleAuthRequestDTO> {

    private final GoogleTokenVerificationService googleTokenVerificationService;
    private final UserService userService;
    private final SessionIssuer sessionIssuer;

    public GoogleAuthenticationService(GoogleTokenVerificationService googleTokenVerificationService,
                                       UserService userService,
                                       SessionIssuer sessionIssuer) {
        this.googleTokenVerificationService = googleTokenVerificationService;
        this.userService = userService;
        this.sessionIssuer = sessionIssuer;
    }

    @Override
    public Class<GoogleAuthRequestDTO> supports() {
        return GoogleAuthRequestDTO.class;
    }

    @Override
    public AuthResponseDTO authenticate(GoogleAuthRequestDTO request) {
        String idTokenString = request.getIdToken();
        GoogleIdToken.Payload payload = googleTokenVerificationService.verifyToken(idTokenString);
        if (payload == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        String email = payload.getEmail();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String picture = (String) payload.get("picture");

        boolean isNewUser = !userService.existsByEmail(email);
        User user = userService.createIfNotExists(firstName, lastName, email, picture, AuthProvider.GOOGLE, true);

        if (user == null) {
            throw new RuntimeException("User creation failed");
        }

        return sessionIssuer.issueSession(user, AuthProvider.GOOGLE, isNewUser);
    }
}