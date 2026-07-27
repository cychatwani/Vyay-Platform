package com.vyay.core.dto.response.group;

import com.vyay.core.entity.group.GroupInviteLink;
import com.vyay.core.enums.InviteLinkType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class InviteSummaryDTO {
    private UUID inviteId;
    private InviteLinkType type;
    private String url;            // shareable join URL — assembled from frontend base + invite code
    private Instant expiresAt;
    private Integer maxUses;
    private Integer useCount;
    private Instant createdAt;

    /**
     * Builds the shareable URL by appending /join/{code} to the configured frontend base URL.
     * The base is normalised so a trailing slash on either side doesn't produce //.
     */
    public static InviteSummaryDTO from(GroupInviteLink l, String frontendBaseUrl) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String url = base + "/join/" + l.getCode();

        return InviteSummaryDTO.builder()
                .inviteId(l.getId())
                .type(l.getType())
                .url(url)
                .expiresAt(l.getExpiresAt())
                .maxUses(l.getMaxUses())
                .useCount(l.getUseCount())
                .createdAt(l.getCreatedAt())
                .build();
    }
}