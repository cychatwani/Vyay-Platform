package com.vyay.core.dto.requests.group;

import com.vyay.core.enums.GroupType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRequestDTO {

    @NotBlank(message = "Group name is required")
    @Size(
            min = 3,
            max = 50,
            message = "Group name must be between 3 and 50 characters"
    )

    private String name;

    @Size(
            max = 250,
            message = "Description cannot exceed 250 characters"
    )
    private String description;

    @NotNull(message = "Group type is required")
    private GroupType type;

    @NotBlank(message = "Default currency code is required")
    @Pattern(
            regexp = "^[A-Z]{3}$",
            message = "Currency code must be a valid ISO 4217 code format (e.g. USD, INR)"
    )
    private String defaultCurrencyCode;

    @AssertTrue(message = "INDIVIDUAL groups cannot be created directly")
    public boolean isGroupTypeValid() {
        return type != GroupType.INDIVIDUAL;
    }
}