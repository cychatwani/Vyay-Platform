package com.vyay.core.dto.requests.profile;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {

    private String defaultCurrency;
    private String defaultLanguage;
    private String preferences;
}