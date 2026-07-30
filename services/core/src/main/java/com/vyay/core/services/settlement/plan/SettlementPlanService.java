package com.vyay.core.services.settlement.plan;

import com.vyay.core.dto.response.settlement.SettlementPlanLineResponseDTO;
import com.vyay.core.dto.response.settlement.SettlementPlanResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.entity.settlement.SettlementPlan;
import com.vyay.core.entity.settlement.SettlementPlanLine;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SettlementPlanStatus;
import com.vyay.core.exception.business.AdminOnlyException;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.InvalidSettlementException;
import com.vyay.core.exception.business.LedgerDriftException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.exception.business.SettlementPlanConflictException;
import com.vyay.core.exception.business.SettlementPlanNotFoundException;
import com.vyay.core.repository.BalanceRepository;
import com.vyay.core.repository.CurrencyRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.SettlementPlanLineRepository;
import com.vyay.core.repository.SettlementPlanRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.repository.projection.PlanLineProgressView;
import com.vyay.core.services.settlement.plan.SettlementPlanCalculator.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * User-initiated settlement-plan operations: generate, get, cancel.
 *
 * A plan is generated from the group's current per-currency balances (all users,
 * including departed ones, so the nets sum to zero) netted by
 * {@link SettlementPlanCalculator}. Generate doubles as regenerate: it always
 * produces a fresh plan, superseding whatever currently holds the live slot, so a
 * caller never has to cancel first.
 *
 * How that sits next to the append/stale mechanism (in ExpenseService /
 * SettlementService): staleness adjusts the live plan in place as balances move,
 * whereas regeneration replaces it wholesale. Fulfilment recorded against the old
 * plan's lines stays with the old plan and does NOT carry over — confirmed
 * settlements are already reflected in the balances the new plan is netted from,
 * but settlements still PROPOSED remain linked to the superseded plan's lines.
 */
@Service
public class SettlementPlanService {

    private static final Logger log = LoggerFactory.getLogger(SettlementPlanService.class);

    /** The "live slot": at most one plan per (group, currency) is in one of these. */
    private static final List<SettlementPlanStatus> LIVE =
            List.of(SettlementPlanStatus.ACTIVE, SettlementPlanStatus.STALE);

    private final SettlementPlanRepository planRepository;
    private final SettlementPlanLineRepository lineRepository;
    private final BalanceRepository balanceRepository;
    private final CurrencyRepository currencyRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;

    public SettlementPlanService(SettlementPlanRepository planRepository,
                                 SettlementPlanLineRepository lineRepository,
                                 BalanceRepository balanceRepository,
                                 CurrencyRepository currencyRepository,
                                 GroupRepository groupRepository,
                                 GroupMembershipRepository membershipRepository,
                                 UserRepository userRepository) {
        this.planRepository = planRepository;
        this.lineRepository = lineRepository;
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
     * Open to any active member, matching a first-time generate — note this means
     * a non-admin can discard the group's live plan here, whereas {@link #cancel}
     * is admin-only.
     */
    @Transactional
    public SettlementPlanResponseDTO generate(User principal, UUID groupId, String currencyCode) {
        Group group = groupRepository.findById(groupId)
                .filter(g -> !g.isDeleted())
                .orElseThrow(GroupNotFoundException::new);
        Currency currency = resolveCurrency(currencyCode);
        requireActiveMember(groupId, principal.getId());

        // Netted up front, before anything is written: a drift or nothing-to-settle
        // failure then aborts without having disturbed the plan already in place.
        List<Transfer> transfers = netCurrentBalances(groupId, currency.getId());
        if (transfers.isEmpty()) {
            throw new InvalidSettlementException("There is nothing to settle in this group for this currency.");
        }

        UUID replacedPlanId = vacateLiveSlot(groupId, currency.getId());
        SettlementPlan plan = insertPlan(group, currency, principal, transfers);

        if (replacedPlanId != null) {
            // Step 3, now that the replacement row exists and the self-FK resolves.
            planRepository.linkSuperseded(replacedPlanId, plan.getId());
            log.info("Regenerated settlement plan for group={} currency={}: plan {} superseded by {}",
                    groupId, currency.getId(), replacedPlanId, plan.getId());
        }

        return read(plan);
    }

    /** Each user's net position for this currency, netted down to directed transfers. */
    private List<Transfer> netCurrentBalances(UUID groupId, UUID currencyId) {
        Map<UUID, Long> netByUser = new HashMap<>();
        for (Balance b : balanceRepository.findByGroupIdAndCurrencyId(groupId, currencyId)) {
            netByUser.merge(b.getUser().getId(), b.getNetAmountMinor(), Long::sum);
        }
        try {
            return SettlementPlanCalculator.plan(netByUser);
        } catch (IllegalStateException e) {
            // Non-zero sum = ledger drift. Log the context, hide it from the client.
            log.error("Ledger drift while generating settlement plan for group={} currency={}: {}",
                    groupId, currencyId, e.getMessage());
            throw new LedgerDriftException();
        }
    }

    /**
     * Regeneration step 1: move whatever holds the live slot to SUPERSEDED, and
     * return its id (null when the slot was empty). The partial unique index covers
     * STALE as well as ACTIVE, so the old plan has to leave before the new row goes
     * in — which is why this runs in the same transaction as the insert.
     */
    private UUID vacateLiveSlot(UUID groupId, UUID currencyId) {
        SettlementPlan live = planRepository
                .findByGroupAndCurrencyAndStatusIn(groupId, currencyId, LIVE)
                .orElse(null);
        if (live == null) {
            return null;
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

    private SettlementPlan insertPlan(Group group, Currency currency, User principal, List<Transfer> transfers) {
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

        List<SettlementPlanLine> lines = transfers.stream()
                .map(t -> newLine(plan, t))
                .toList();
        lineRepository.saveAll(lines);
        return plan;
    }

    private SettlementPlanLine newLine(SettlementPlan plan, Transfer t) {
        return SettlementPlanLine.builder()
                .plan(plan)
                .fromUser(userRepository.getReferenceById(t.fromUserId()))
                .toUser(userRepository.getReferenceById(t.toUserId()))
                .amountMinor(t.amountMinor())
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
        List<SettlementPlanLineResponseDTO> lines = new java.util.ArrayList<>();
        for (PlanLineProgressView v : lineRepository.findProgressByPlanId(plan.getId())) {
            lines.add(SettlementPlanLineResponseDTO.from(v, currency, plan.getCreatedAt()));
        }
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
