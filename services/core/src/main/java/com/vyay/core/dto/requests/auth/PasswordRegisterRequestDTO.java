package com.vyay.core.dto.requests.auth;

import com.vyay.core.enums.PasswordRegistrationVerificationChannel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordRegisterRequestDTO implements BasePasswordRegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z]+(?:[ '-][a-zA-Z]+)*$",
            message = "First name contains invalid characters"
    )
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z]+(?:[ '-][a-zA-Z]+)*$",
            message = "Last name contains invalid characters"
    )
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Verification type is required")
    private PasswordRegistrationVerificationChannel verificationType;
}