package com.vyay.core.dto.requests.settlement;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Body for POST /record — "{fromUser} paid {toUser}". Both parties are named
 * because the authenticated user need not be either; recording a settlement
 * between two OTHER members is gated by the group's third-party settlement
 * policy. When the principal IS a party, this behaves like /paid or /received.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecordSettlementRequestDTO extends SettlementRequestBase {

    @NotNull(message = "fromUserId is required")
    private UUID fromUserId;

    @NotNull(message = "toUserId is required")
    private UUID toUserId;
}
