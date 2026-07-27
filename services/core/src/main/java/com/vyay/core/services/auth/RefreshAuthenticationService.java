package com.vyay.core.services.auth;

import com.vyay.core.dto.requests.auth.RefreshTokenAuthRequestDto;
import com.vyay.core.dto.response.Auth.AuthResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.enums.AuthProvider;
import com.vyay.core.enums.TokenType;
import com.vyay.core.security.JwtService;
import com.vyay.core.security.TokenStore;
import com.vyay.core.services.user.UserService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public final class RefreshAuthenticationService implements AuthenticationProvider<RefreshTokenAuthRequestDto> {

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final UserService userService;
    private final SessionIssuer sessionIssuer;

    public RefreshAuthenticationService(UserService userService,
                                        JwtService jwtService,
                                        TokenStore tokenStore,
                                        SessionIssuer sessionIssuer) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.sessionIssuer = sessionIssuer;
    }

    @Override
    public Class<RefreshTokenAuthRequestDto> supports() {
        return RefreshTokenAuthRequestDto.class;
    }

    @Override
    public AuthResponseDTO authenticate(RefreshTokenAuthRequestDto request) {
        String refreshToken = request.getRefreshToken();

        UUID userId = jwtService.getUserIdFromToken(refreshToken, TokenType.REFRESH.getValue());

        String storedToken = tokenStore.getRefreshToken(userId)
                .orElseThrow(() -> new RuntimeException("Refresh token expired or no longer valid"));

        if (!storedToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token expired or no longer valid");
        }

        User user = userService.getById(userId)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        return sessionIssuer.issueSession(user, AuthProvider.REFRESH, false);
    }
}
