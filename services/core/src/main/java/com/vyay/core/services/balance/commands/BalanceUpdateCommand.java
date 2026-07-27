package com.vyay.core.services.balance.commands;

import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.LedgerSourceType;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable value object describing a set of per-user balance deltas to apply,
 * scoped to one (group, currency) and traceable to a single ledger source.
 * <p>
 * Pure data: knows nothing about the domain objects that produce it. Translation
 * from Expense / Settlement / etc. lives in {@link BalanceUpdateCommandFactory}.
 */
@Getter
public final class BalanceUpdateCommand {

    private final Group group;
    private final Currency currency;
    private final LedgerSourceType sourceType;
    private final UUID sourceId;
    private final Map<UUID, Long> userDeltas;

    // Package-private: only BalanceUpdateCommandFactory (same package) constructs these.
    BalanceUpdateCommand(Group group,
                         Currency currency,
                         LedgerSourceType sourceType,
                         UUID sourceId,
                         Map<UUID, Long> userDeltas) {
        this.group = group;
        this.currency = currency;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.userDeltas = Map.copyOf(userDeltas);
    }
}