package com.vyay.core.dto.requests.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public final class PasswordAuthRequestDTO implements AuthenticationRequest {
    @NotBlank
    String email;
    @NotBlank
    String password;
}
