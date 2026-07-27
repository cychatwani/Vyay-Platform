package com.vyay.core.dto.response.User;

import com.vyay.core.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserSummaryDTO {
    private UUID userId;
    private String displayName;

    public static UserSummaryDTO from(User user) {
        return UserSummaryDTO.builder()
                .userId(user.getId())
                .displayName(user.getFullName())
                .build();
    }
}