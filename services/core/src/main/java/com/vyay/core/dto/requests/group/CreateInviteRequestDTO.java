package com.vyay.core.dto.requests.group;

import com.vyay.core.enums.InviteLinkType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInviteRequestDTO {

    @NotNull
    private InviteLinkType type;

    private List<UUID> invitedUserIds;

    private Instant expiresAt;
    private Integer maxUses;

    @AssertTrue(message = "PRIMARY invites cannot set invited users, expiresAt or maxUses")
    public boolean isValidForType() {
        if (type != InviteLinkType.PRIMARY) {
            return true;
        }

        return (invitedUserIds == null || invitedUserIds.isEmpty())
                && expiresAt == null
                && maxUses == null;
    }
}
