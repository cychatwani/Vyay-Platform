package com.vyay.core.dto.requests.auth;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public final class RefreshTokenAuthRequestDto implements AuthenticationRequest {
    @NotBlank
    String refreshToken;
}
