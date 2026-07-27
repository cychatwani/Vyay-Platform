package com.vyay.core.dto.requests.group;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JoinGroupRequestDTO {

    @NotBlank(message = "token is required")
    private String token;
}