package com.vyay.core.services.auth;

import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.dto.response.User.UserDetailsDTO;
import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.security.JwtService;
import com.vyay.core.security.TokenStore;
import com.vyay.core.services.profile.UserProfileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessionIssuer {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final UserProfileService userProfileService;
    private final long refreshExpiration;

    public SessionIssuer(JwtService jwtService,
                         TokenStore tokenStore,
                         UserProfileService userProfileService,
                         @Value("${spring.jwt.refresh-token-expiration}") long refreshExpiration) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.userProfileService = userProfileService;
        this.refreshExpiration = refreshExpiration;
    }

    public AuthResponseDTO issueSession(User user, AuthProvider authType, boolean isNewUser) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        tokenStore.storeRefreshToken(user.getId(), refreshToken, refreshExpiration);

        UserDetailsDTO userDetails = UserDetailsDTO.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .profilePicture(user.getProfilePicture())
                .email(user.getEmail())
                .userId(user.getId())
                .hasProfile(userProfileService.hasProfile(user.getId()))
                .build();

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authType(authType)
                .isNewUser(isNewUser)
                .userDetails(userDetails)
                .build();
    }
}
