package com.vyay.core.services.balance.commands;

import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.LedgerSourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Translates domain objects into {@link BalanceUpdateCommand}s. Pure functions:
 * no lifecycle enforcement, no persistence. Callers are responsible for invoking
 * these only when a balance change is actually warranted (e.g. the service only
 * builds a settlement command once the settlement is CONFIRMED).
 */
public final class BalanceUpdateCommandFactory {

    private BalanceUpdateCommandFactory() {
    }

    /**
     * Net-per-user deltas for an expense: each payer credited what they paid,
     * each participant debited what they owe. Zero-net users are dropped so they
     * never produce a no-op balance row or ledger entry.
     */
    public static BalanceUpdateCommand from(Expense expense) {
        Map<UUID, Long> userDeltas = new HashMap<>();

        expense.getPayers().forEach(payer ->
                userDeltas.merge(
                        payer.getUser().getId(),
                        payer.getAmountPaidMinor(),
                        Long::sum
                )
        );

        expense.getShares().forEach(share ->
                userDeltas.merge(
                        share.getUser().getId(),
                        -share.getOwedAmountMinor(),
                        Long::sum
                )
        );

        userDeltas.values().removeIf(delta -> delta == 0);

        return new BalanceUpdateCommand(
                expense.getGroup(),
                expense.getCurrency(),
                LedgerSourceType.EXPENSE,
                expense.getId(),
                userDeltas
        );
    }

    /**
     * Deltas for a settlement (a real transfer fromUser -> toUser that already
     * happened). Sign convention matches Balance: a positive net means the group
     * owes the user (creditor), negative means the user owes the group (debtor).
     * Recording the payment moves both parties toward zero:
     *   fromUser (debtor)   += amount  (their negative balance rises toward 0)
     *   toUser   (creditor) -= amount  (their positive balance falls toward 0)
     * <p>
     * Trusts the caller to only pass a CONFIRMED settlement; enforces no lifecycle.
     */
    public static BalanceUpdateCommand from(Settlement settlement) {
        Map<UUID, Long> userDeltas = new HashMap<>();

        long amount = settlement.getAmountMinor();
        userDeltas.merge(settlement.getFromUser().getId(), amount, Long::sum);
        userDeltas.merge(settlement.getToUser().getId(), -amount, Long::sum);

        userDeltas.values().removeIf(delta -> delta == 0);

        return new BalanceUpdateCommand(
                settlement.getGroup(),
                settlement.getCurrency(),
                LedgerSourceType.SETTLEMENT,
                settlement.getId(),
                userDeltas
        );
    }
}