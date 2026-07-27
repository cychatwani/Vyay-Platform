package com.vyay.core.dto.requests.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public final class GoogleAuthRequestDTO implements AuthenticationRequest {
    @NotBlank()
    String idToken;
}
