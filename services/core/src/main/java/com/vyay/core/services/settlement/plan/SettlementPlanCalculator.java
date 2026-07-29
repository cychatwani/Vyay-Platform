package com.vyay.core.services.settlement.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

/**
 * Nets a group's per-user balances (for a single currency) into a set of
 * directed transfers that clear everyone to zero.
 *
 * Pure function, no persistence and no domain coupling — same spirit as the
 * balance command factory. Input is the net-minor position per user under the
 * Balance sign convention: {@code net > 0} means the group owes the user
 * (creditor), {@code net < 0} means the user owes the group (debtor). Zero-net
 * users may be present or absent; both are handled.
 *
 * <h3>Algorithm</h3>
 * Greedy: repeatedly settle the largest creditor against the largest debtor,
 * transferring {@code min(credit, debt)} and pushing back whatever remains. Each
 * transfer zeroes at least one party, so it emits at most {@code n - 1} transfers
 * for {@code n} non-zero participants.
 *
 * This is NOT guaranteed minimal. Minimising the number of transactions that
 * settle a set of debts is NP-hard (it contains subset-sum: a zero-sum subset of
 * participants could settle among themselves in fewer transfers than greedy
 * finds). Greedy trades optimality for an O(n log n) result that is good enough
 * and, crucially, stable.
 *
 * <h3>Determinism</h3>
 * The heaps order by amount descending, ties broken by ascending userId. Since
 * userIds are unique this is a total order, so there are never two
 * indistinguishable candidates and the output is fully determined by the input.
 * The same balances always yield a byte-identical plan — which is what lets a
 * regenerated plan equal the remainder of a partially-executed one
 * (self-stability): applying a transfer moves the balances to exactly the state
 * greedy would already be in after emitting it.
 *
 * The caller MUST pass a balanced position (nets sum to zero). A non-zero sum is
 * ledger drift; this throws {@link IllegalStateException} rather than emit a
 * bogus plan. The service maps that to the typed, generically-messaged
 * ledger-drift error and logs the group/currency context.
 */
public final class SettlementPlanCalculator {

    private SettlementPlanCalculator() {
    }

    /** A proposed transfer: {@code fromUserId} (debtor) pays {@code toUserId} (creditor). */
    public record Transfer(UUID fromUserId, UUID toUserId, long amountMinor) {
    }

    private record Party(UUID userId, long amount) {
    }

    /** Largest amount first; ties broken by ascending userId (a total order). */
    private static final Comparator<Party> LARGEST_FIRST =
            Comparator.comparingLong(Party::amount).reversed()
                    .thenComparing(Party::userId);

    public static List<Transfer> plan(Map<UUID, Long> netByUser) {
        PriorityQueue<Party> creditors = new PriorityQueue<>(LARGEST_FIRST);
        PriorityQueue<Party> debtors = new PriorityQueue<>(LARGEST_FIRST);

        long sum = 0;
        for (Map.Entry<UUID, Long> e : netByUser.entrySet()) {
            long net = e.getValue();
            sum += net;
            if (net > 0) {
                creditors.add(new Party(e.getKey(), net));
            } else if (net < 0) {
                debtors.add(new Party(e.getKey(), -net));   // store the debt magnitude, positive
            }
        }

        if (sum != 0) {
            throw new IllegalStateException(
                    "Balances do not net to zero (residual " + sum + " minor units)");
        }

        List<Transfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Party creditor = creditors.poll();
            Party debtor = debtors.poll();

            long amount = Math.min(creditor.amount(), debtor.amount());
            transfers.add(new Transfer(debtor.userId(), creditor.userId(), amount));

            long creditLeft = creditor.amount() - amount;
            long debtLeft = debtor.amount() - amount;
            if (creditLeft > 0) {
                creditors.add(new Party(creditor.userId(), creditLeft));
            }
            if (debtLeft > 0) {
                debtors.add(new Party(debtor.userId(), debtLeft));
            }
        }

        return transfers;
    }
}
