package com.vyay.core.controllers;

import com.vyay.core.dto.requests.profile.CreateProfileRequestDTO;
import com.vyay.core.dto.requests.profile.UpdateProfileRequestDTO;
import com.vyay.core.dto.response.profile.ProfileResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.entity.UserProfile;
import com.vyay.core.services.profile.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final UserProfileService userProfileService;

    public ProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateProfileRequestDTO request) {
        userProfileService.createProfile(user, request.getDefaultCurrency(), request.getDefaultLanguage());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Profile created successfully."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> getProfile(
            @AuthenticationPrincipal User user) {
        UserProfile profile = userProfileService.getProfile(user.getId());
        return ResponseEntity.ok(ApiResponse.success(ProfileResponseDTO.from(profile)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileResponseDTO>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateProfileRequestDTO request) {
        UserProfile profile = userProfileService.updateProfile(user, request);
        return ResponseEntity.ok(ApiResponse.success(ProfileResponseDTO.from(profile)));
    }
}