package com.vyay.core.dto.response.Auth;

import com.vyay.core.dto.response.User.UserDetailsDTO;
import com.vyay.core.enums.AuthProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private AuthProvider authType;
    private Boolean isNewUser;
    private UserDetailsDTO userDetails;
}
