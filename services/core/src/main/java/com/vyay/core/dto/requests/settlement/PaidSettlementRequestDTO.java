package com.vyay.core.dto.requests.settlement;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Body for POST /paid — "I paid {toUser}". The authenticated user is the payer
 * (fromUser), so only the payee is named. Field name matches the entity's
 * toUser directly.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaidSettlementRequestDTO extends SettlementRequestBase {

    @NotNull(message = "toUserId is required")
    private UUID toUserId;
}
