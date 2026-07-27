package com.vyay.core.dto.requests.settlement;

import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Body for POST /received — "{fromUser} paid me". The authenticated user is the
 * payee (toUser), so only the payer is named. Field name matches the entity's
 * fromUser directly.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ReceivedSettlementRequestDTO extends SettlementRequestBase {

    @NotNull(message = "fromUserId is required")
    private UUID fromUserId;
}
