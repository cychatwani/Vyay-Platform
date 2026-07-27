package com.vyay.core.dto.requests.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProfileRequestDTO {

    @NotBlank
    private String defaultCurrency;

    @NotBlank
    private String defaultLanguage;
}