package com.vyay.core.services.auth;

import com.vyay.core.dto.requests.auth.AuthenticationRequest;
import com.vyay.core.dto.response.Auth.AuthResponseDTO;

public interface AuthenticationService {
    AuthResponseDTO authenticate(AuthenticationRequest request);
}
