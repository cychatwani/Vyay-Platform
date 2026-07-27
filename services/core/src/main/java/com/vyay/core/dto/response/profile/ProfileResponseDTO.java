package com.vyay.core.dto.response.profile;

import com.vyay.core.entity.UserProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponseDTO {

    private String defaultCurrency;
    private String language;
    private String preferences;

    public static ProfileResponseDTO from(UserProfile profile) {
        return ProfileResponseDTO.builder()
                .defaultCurrency(profile.getDefaultCurrency().getCode())
                .language(profile.getLanguage().getCode())
                .preferences(profile.getPreferences())
                .build();
    }
}