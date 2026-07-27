package com.vyay.core.dto.response.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vyay.core.entity.User;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.enums.GroupRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MemberDTO {
    private UUID userId;
    private String displayName;
    private String profilePicture;
    private GroupRole role;
    private Instant activeSince;
    private List<CurrencyBalanceDTO> balances;

    @Getter(onMethod_ = @JsonProperty("isCurrentUser"))
    private boolean isCurrentUser;

    /**
     * Viewer-aware mapping. Balances are supplied by the caller (they require a
     * cross-member currency set the single membership doesn't carry) and are
     * zero-filled across every currency the group touches.
     */
    public static MemberDTO from(GroupMembership m, UUID currentUserId,
                                 List<CurrencyBalanceDTO> balances) {
        User u = m.getUser();
        return MemberDTO.builder()
                .userId(u.getId())
                .displayName(u.getFullName())
                .profilePicture(u.getProfilePicture())
                .role(m.getRole())
                .activeSince(m.getActiveSince())
                .balances(balances)
                .isCurrentUser(currentUserId != null && currentUserId.equals(u.getId()))
                .build();
    }
}