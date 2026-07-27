package com.vyay.core.dto.requests.auth;

public sealed interface AuthenticationRequest
        permits GoogleAuthRequestDTO, RefreshTokenAuthRequestDto, PasswordAuthRequestDTO  {
}
