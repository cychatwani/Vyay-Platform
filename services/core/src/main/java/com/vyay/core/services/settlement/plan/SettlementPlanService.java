package com.vyay.core.services.settlement.plan;

import com.vyay.core.dto.response.settlement.SettlementPlanLineResponseDTO;
import com.vyay.core.dto.response.settlement.SettlementPlanResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.entity.settlement.SettlementPlan;
import com.vyay.core.entity.settlement.SettlementPlanLine;
import com.vyay.core.entity.settlement.SettlementPlanLineFulfillment;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SettlementPlanStatus;
import com.vyay.core.exception.business.AdminOnlyException;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.LedgerDriftException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.exception.business.NothingToSettleException;
import com.vyay.core.exception.business.PendingSettlementExceedsDebtException;
import com.vyay.core.exception.business.SettlementPlanConflictException;
import com.vyay.core.exception.business.SettlementPlanNotFoundException;
import com.vyay.core.repository.BalanceRepository;
import com.vyay.core.repository.CurrencyRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.SettlementPlanLineFulfillmentRepository;
import com.vyay.core.repository.SettlementPlanLineRepository;
import com.vyay.core.repository.SettlementPlanRepository;
import com.vyay.core.repository.SettlementRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.services.settlement.plan.SettlementPlanCalculator.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * User-initiated settlement-plan operations: generate, get, cancel.
 *
 * A plan nets a group's per-currency balances (all users, including departed
 * ones, so the nets sum to zero) into directed transfers. Generate doubles as
 * regenerate: it always produces a fresh plan, superseding whatever currently
 * holds the live slot, so a caller never has to cancel first.
 *
 * <h2>In-flight settlements: adjusted basis + carried line</h2>
 * Balances move only when a settlement is CONFIRMED, so a PROPOSED settlement is
 * money that has been promised but is invisible to {@code balances}. Generation
 * handles it in two halves that fit together exactly:
 *
 * <ol>
 *   <li>the basis is adjusted as if every pending settlement had already
 *       confirmed — {@code basis[payer] += amount}, {@code basis[payee] -= amount}
 *       — so the calculator routes only what is left over;</li>
 *   <li>each pending settlement gets a CARRIED line of its own, at its exact
 *       from / to / amount, linked to it at the full amount.</li>
 * </ol>
 *
 * The carried line puts back precisely what the basis adjustment took out, so the
 * two lists together cover the real balances exactly — always, for any set of
 * pending settlements. That is the property the previous design could not hold:
 * planning from raw balances and then attaching settlements to whichever netted
 * lines happened to match left promised money that no line could absorb, because
 * line capacity is per-pair and decided by the netting while settlement creation
 * validates one proposal at a time against the payer's whole net balance.
 *
 * Nothing is double-counted, because the carried line is not an extra obligation
 * — it IS the settlement. Fully pending, nothing remaining, so the plan shows the
 * payer that this money is already on its way and never asks for it again. When
 * it confirms, the same line simply reads fulfilled.
 *
 * The one input this cannot absorb is a settlement promising more than its payer
 * owes the group at all; see {@link #requireNoCarriedReversal}.
 *
 * CONFIRMED settlements are never touched: that money already moved the balances
 * these lines are planned from, and re-linking it would count it twice. Their
 * links stay on the superseded plan, which is real history.
 */
@Service
public class SettlementPlanService {

    private static final Logger log = LoggerFactory.getLogger(SettlementPlanService.class);

    /** The "live slot": at most one plan per (group, currency) is in one of these. */
    private static final List<SettlementPlanStatus> LIVE =
            List.of(SettlementPlanStatus.ACTIVE, SettlementPlanStatus.STALE);

    private final SettlementPlanRepository planRepository;
    private final SettlementPlanLineRepository lineRepository;
    private final SettlementPlanLineFulfillmentRepository fulfillmentRepository;
    private final SettlementRepository settlementRepository;
    private final BalanceRepository balanceRepository;
    private final CurrencyRepository currencyRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public SettlementPlanService(SettlementPlanRepository planRepository,
                                 SettlementPlanLineRepository lineRepository,
                                 SettlementPlanLineFulfillmentRepository fulfillmentRepository,
                                 SettlementRepository settlementRepository,
                                 BalanceRepository balanceRepository,
                                 CurrencyRepository currencyRepository,
                                 GroupRepository groupRepository,
                                 GroupMembershipRepository membershipRepository,
                                 UserRepository userRepository) {
        this.planRepository = planRepository;
        this.lineRepository = lineRepository;
        this.fulfillmentRepository = fulfillmentRepository;
        this.settlementRepository = settlementRepository;
        this.balanceRepository = balanceRepository;
        this.currencyRepository = currencyRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
    }

    /**
     * Generate the (single) live plan for a group + currency, replacing one if it
     * is already there. Any live (ACTIVE or STALE) plan is moved to SUPERSEDED and
     * pointed forward at its replacement, keeping the regenerate chain traceable
     * and the live slot occupied by exactly one row throughout.
     *
     * Every caller must be an active member; beyond that, who may generate is
     * decided by what generating would destroy — see {@link #vacateLiveSlot}.
     */
    @Transactional
    public SettlementPlanResponseDTO generate(User principal, UUID groupId, String currencyCode) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(GroupNotFoundException::new);

        Currency currency = resolveCurrency(currencyCode);

        requireActiveMember(groupId, principal.getId());

        List<Settlement> pending = settlementRepository.findOutstandingProposed(groupId, currency.getId());

        // Everything that can refuse the request runs before anything is written, so
        // a drift / nothing-to-settle / bad-settlement failure aborts without having
        // disturbed the plan already in place.
        List<Transfer> transfers = netAdjustedBasis(groupId, currency.getId(), pending);
        requireNoCarriedReversal(transfers, pending);
        if (transfers.isEmpty() && pending.isEmpty()) {
            throw new NothingToSettleException();
        }

        UUID replacedPlanId = vacateLiveSlot(groupId, currency.getId(), principal.getId());
        SettlementPlan plan = insertPlan(group, currency, principal, transfers, pending);

        if (replacedPlanId != null) {
            // Step 3, now that the replacement row exists and the self-FK resolves.
            planRepository.linkSuperseded(replacedPlanId, plan.getId());
            log.info("Regenerated settlement plan for group={} currency={}: plan {} superseded by {}"
                            + " ({} pending settlement(s) carried forward)",
                    groupId, currency.getId(), replacedPlanId, plan.getId(), pending.size());
        }

        return read(plan);
    }

    /**
     * The transfers still needed once every in-flight settlement is taken as good:
     * each user's net position, moved by each pending settlement as though it had
     * confirmed, then netted down by the calculator.
     *
     * The adjustment is the exact inverse of what confirming would do — the payer's
     * debt shrinks ({@code += amount} under the Balance sign convention, where a
     * negative net means the user owes) and the payee's credit shrinks — so the
     * adjusted basis still sums to zero and cannot manufacture drift on its own.
     */
    private List<Transfer> netAdjustedBasis(UUID groupId, UUID currencyId, List<Settlement> pending) {
        Map<UUID, Long> basis = new HashMap<>();
        for (Balance b : balanceRepository.findByGroupIdAndCurrencyId(groupId, currencyId)) {
            basis.merge(b.getUser().getId(), b.getNetAmountMinor(), Long::sum);
        }
        for (Settlement s : pending) {
            basis.merge(s.getFromUser().getId(), s.getAmountMinor(), Long::sum);
            basis.merge(s.getToUser().getId(), -s.getAmountMinor(), Long::sum);
        }
        try {
            return SettlementPlanCalculator.plan(basis);
        } catch (IllegalStateException e) {
            // Non-zero sum = ledger drift. Log the context, hide it from the client.
            log.error("Ledger drift while generating settlement plan for group={} currency={}: {}",
                    groupId, currencyId, e.getMessage());
            throw new LedgerDriftException();
        }
    }

    /**
     * Refuse to build a plan around a settlement that promises more than its payer
     * owes.
     *
     * Such a settlement pushes its payer past zero in the adjusted basis, turning
     * them into a creditor, so the calculator routes money BACK to them — and if it
     * routes that money from the very person they were paying, the plan holds a
     * carried line one way and a netted line the other way between the same two
     * people. Refusing beats persisting that: the settlement is what is wrong, and
     * only cancelling it makes the group plannable again.
     *
     * Reachable because creation validates the payer's debt at proposal time and
     * nothing re-checks it while the proposal waits — an expense or another
     * confirmation can shrink that debt underneath it.
     */
    private void requireNoCarriedReversal(List<Transfer> transfers, List<Settlement> pending) {
        if (pending.isEmpty() || transfers.isEmpty()) {
            return;
        }
        Set<Map.Entry<UUID, UUID>> carriedPairs = new HashSet<>();
        for (Settlement s : pending) {
            carriedPairs.add(pairOf(s.getFromUser().getId(), s.getToUser().getId()));
        }
        for (Transfer t : transfers) {
            // The netted line runs to -> from; reversed, that is a carried pair.
            if (carriedPairs.contains(pairOf(t.toUserId(), t.fromUserId()))) {
                throw new PendingSettlementExceedsDebtException(offender(pending, t));
            }
        }
    }

    /** The carried settlement whose direction {@code t} reverses. */
    private static UUID offender(List<Settlement> pending, Transfer t) {
        return pending.stream()
                .filter(s -> s.getFromUser().getId().equals(t.toUserId())
                        && s.getToUser().getId().equals(t.fromUserId()))
                .findFirst()
                .map(Settlement::getId)
                .orElseThrow(IllegalStateException::new);   // unreachable: the pair came from this list
    }

    /**
     * Regeneration step 1: move whatever holds the live slot to SUPERSEDED, and
     * return its id (null when the slot was empty). The partial unique index covers
     * STALE as well as ACTIVE, so the old plan has to leave before the new row goes
     * in — which is why this runs in the same transaction as the insert.
     *
     * Also the authorization point for {@link #generate}, because the plan read here
     * is the thing being destroyed and its status is what decides who may destroy it:
     * <ul>
     *   <li>empty slot — any active member; a first generate is normal workflow.</li>
     *   <li>STALE — any active member. A stale plan is read-only and can only be
     *       left by regenerating; requiring an admin would strand the whole group
     *       until one turns up.</li>
     *   <li>ACTIVE — admins only. Discarding a live plan the group is working
     *       through is the same act as {@link #cancel}, so it carries the same bar.</li>
     * </ul>
     */
    private UUID vacateLiveSlot(UUID groupId, UUID currencyId, UUID actorId) {
        SettlementPlan live = planRepository
                .findByGroupAndCurrencyAndStatusIn(groupId, currencyId, LIVE)
                .orElse(null);
        if (live == null) {
            return null;
        }
        if (live.getStatus() == SettlementPlanStatus.ACTIVE) {
            requireActiveAdmin(groupId, actorId);
        }
        if (planRepository.supersede(live.getId()) == 0) {
            // Guarded CAS lost: a concurrent cancel / completion / regeneration
            // already moved this plan out of the live slot, so whatever sits there
            // now is not the plan we read and not ours to replace. Bail instead of
            // racing the unique index.
            throw new SettlementPlanConflictException();
        }
        return live.getId();
    }

    private SettlementPlan insertPlan(Group group,
                                      Currency currency,
                                      User principal,
                                      List<Transfer> transfers,
                                      List<Settlement> pending) {
        SettlementPlan plan;
        try {
            // Flushed here so the row exists for linkSuperseded's immediate self-FK.
            plan = planRepository.saveAndFlush(SettlementPlan.builder()
                    .group(group)
                    .currency(currency)
                    .createdBy(userRepository.getReferenceById(principal.getId()))
                    .status(SettlementPlanStatus.ACTIVE)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // Group, currency and principal are all resolved above, so the live-slot
            // partial unique index is the only constraint this insert can realistically
            // trip: a concurrent generate claimed the slot between our read and here.
            throw new SettlementPlanConflictException();
        }

        // Carried lines first, so they take the lower UUIDv7 ids and read at the top
        // of the plan: money already moving, before money still to send. Flushed
        // because the fulfilment rows below carry an FK to them.
        List<SettlementPlanLine> carried = lineRepository.saveAllAndFlush(
                pending.stream().map(s -> carriedLine(plan, s)).toList());
        lineRepository.saveAllAndFlush(
                transfers.stream().map(t -> nettedLine(plan, t)).toList());

        carryForward(carried, pending);
        return plan;
    }

    /**
     * Point each in-flight settlement at its own carried line, at its full amount.
     *
     * One link per settlement, so the line reads exactly as pending as the
     * settlement is real — no splitting, no per-pair capacity, no leftovers. The
     * lists are index-aligned: {@code carried.get(i)} was built from
     * {@code pending.get(i)} and {@code saveAllAndFlush} preserves order.
     */
    private void carryForward(List<SettlementPlanLine> carried, List<Settlement> pending) {
        if (pending.isEmpty()) {
            return;
        }

        // Rebuild the links rather than re-point them — see the repository method for
        // why delete-then-insert handles zero, one and many old links identically.
        // Restricted to PROPOSED ids, so no CONFIRMED fulfilment is ever touched: that
        // money is already in balances and the superseded plan's record of it is real
        // history.
        fulfillmentRepository.deleteBySettlementIdIn(pending.stream().map(Settlement::getId).toList());

        List<SettlementPlanLineFulfillment> links = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            Settlement s = pending.get(i);
            links.add(SettlementPlanLineFulfillment.builder()
                    .planLine(carried.get(i))
                    .settlement(s)
                    .appliedAmountMinor(s.getAmountMinor())
                    .build());
        }
        fulfillmentRepository.saveAll(links);
    }

    private static Map.Entry<UUID, UUID> pairOf(UUID fromUserId, UUID toUserId) {
        return Map.entry(fromUserId, toUserId);
    }

    /** A settlement already in flight, restated as a line of this plan. */
    private SettlementPlanLine carriedLine(SettlementPlan plan, Settlement s) {
        return line(plan, s.getFromUser().getId(), s.getToUser().getId(), s.getAmountMinor());
    }

    /** A transfer the calculator produced from the adjusted basis. */
    private SettlementPlanLine nettedLine(SettlementPlan plan, Transfer t) {
        return line(plan, t.fromUserId(), t.toUserId(), t.amountMinor());
    }

    private SettlementPlanLine line(SettlementPlan plan, UUID fromUserId, UUID toUserId, long amountMinor) {
        return SettlementPlanLine.builder()
                .plan(plan)
                .fromUser(userRepository.getReferenceById(fromUserId))
                .toUser(userRepository.getReferenceById(toUserId))
                .amountMinor(amountMinor)
                // Both kinds are part of generation, so neither is appended.
                .appended(false)
                // Stamped from the plan so the whole generation batch shares one
                // createdAt and sorts as one block, ties broken by id. A line appended
                // later takes its own Instant.now() and lands after all of them.
                .createdAt(plan.getCreatedAt())
                .build();
    }

    /** The current live plan (ACTIVE or STALE) for a group + currency. */
    @Transactional(readOnly = true)
    public SettlementPlanResponseDTO get(User principal, UUID groupId, String currencyCode) {
        Currency currency = resolveCurrency(currencyCode);
        requireActiveMember(groupId, principal.getId());
        SettlementPlan plan = planRepository
                .findByGroupAndCurrencyAndStatusIn(groupId, currency.getId(), LIVE)
                .orElseThrow(SettlementPlanNotFoundException::new);
        return read(plan);
    }

    /** Cancel the live plan — a group artifact, so admins only. */
    @Transactional
    public void cancel(User principal, UUID groupId, String currencyCode) {
        Currency currency = resolveCurrency(currencyCode);
        requireActiveAdmin(groupId, principal.getId());
        SettlementPlan plan = planRepository
                .findByGroupAndCurrencyAndStatusIn(groupId, currency.getId(), LIVE)
                .orElseThrow(SettlementPlanNotFoundException::new);
        // Guarded CAS; a 0 count means a concurrent transition already moved it — no-op.
        planRepository.cancelIfLive(plan.getId());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private SettlementPlanResponseDTO read(SettlementPlan plan) {
        Currency currency = plan.getCurrency();
        List<SettlementPlanLineResponseDTO> lines = lineRepository.findProgressByPlanId(plan.getId()).stream()
                .map(v -> SettlementPlanLineResponseDTO.from(v, currency))
                .toList();
        return SettlementPlanResponseDTO.builder()
                .planId(plan.getId())
                .groupId(plan.getGroup().getId())
                .currencyCode(currency.getCode())
                .currencySymbol(currency.getSymbol())
                .status(plan.getStatus())
                .createdAt(plan.getCreatedAt())
                .lines(lines)
                .build();
    }

    private Currency resolveCurrency(String currencyCode) {
        return currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new InvalidCurrencyException(currencyCode));
    }

    private void requireActiveMember(UUID groupId, UUID userId) {
        if (!membershipRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)) {
            throw new NotAMemberException();
        }
    }

    private void requireActiveAdmin(UUID groupId, UUID userId) {
        GroupMembership membership = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);
        if (membership.getRole() != GroupRole.ADMIN) {
            throw new AdminOnlyException();
        }
    }
}
