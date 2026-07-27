package com.vyay.core.dto.response.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDetailsDTO {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String profilePicture;
    private String email;
    private boolean hasProfile;
}