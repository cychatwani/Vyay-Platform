package com.vyay.core.services.settlement;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.dto.requests.settlement.PaidSettlementRequestDTO;
import com.vyay.core.dto.requests.settlement.ReceivedSettlementRequestDTO;
import com.vyay.core.dto.requests.settlement.RecordSettlementRequestDTO;
import com.vyay.core.dto.response.settlement.SettlementResponseDTO;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.Balance;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.group.GroupMembership;
import com.vyay.core.entity.group.GroupPreferences;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.MembershipStatus;
import com.vyay.core.enums.SettlementMethod;
import com.vyay.core.enums.SettlementStatus;
import com.vyay.core.enums.ThirdPartySettlementPolicy;
import com.vyay.core.exception.business.GroupNotFoundException;
import com.vyay.core.exception.business.InvalidCurrencyException;
import com.vyay.core.exception.business.InvalidSettlementException;
import com.vyay.core.exception.business.NotAMemberException;
import com.vyay.core.exception.business.SettlementForbiddenException;
import com.vyay.core.exception.business.SettlementNotFoundException;
import com.vyay.core.exception.business.SettlementStateException;
import com.vyay.core.repository.BalanceRepository;
import com.vyay.core.repository.CurrencyRepository;
import com.vyay.core.repository.GroupMembershipRepository;
import com.vyay.core.repository.GroupRepository;
import com.vyay.core.repository.SettlementRepository;
import com.vyay.core.repository.UserRepository;
import com.vyay.core.services.balance.BalanceUpdateService;
import com.vyay.core.services.balance.commands.BalanceUpdateCommandFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Owns the settlement lifecycle. A settlement records a real transfer
 * (fromUser -> toUser) that already happened out in the world; the app is a
 * ledger, not a payment rail.
 * <p>
 * Three creation entrypoints funnel into one core:
 *   /paid      -> caller is the payer (debtor)
 *   /received  -> caller is the payee (creditor)
 *   /record    -> explicit payer + payee; caller may be a third party
 * <p>
 * Proposal-time validation (debtor owes &gt;= amount) runs once, at creation, for
 * every path. Confirmation never re-validates: it only asserts "I received it".
 */
@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final BalanceRepository balanceRepository;
    private final CurrencyRepository currencyRepository;
    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final BalanceUpdateService balanceUpdateService;

    public SettlementService(SettlementRepository settlementRepository,
                             BalanceRepository balanceRepository,
                             CurrencyRepository currencyRepository,
                             GroupRepository groupRepository,
                             GroupMembershipRepository membershipRepository,
                             UserRepository userRepository,
                             BalanceUpdateService balanceUpdateService) {
        this.settlementRepository = settlementRepository;
        this.balanceRepository = balanceRepository;
        this.currencyRepository = currencyRepository;
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.balanceUpdateService = balanceUpdateService;
    }

    // ---------------------------------------------------------------------
    // Creation entrypoints
    // ---------------------------------------------------------------------

    // initiatedViaApp is server-set, not client-sent: it records that the payment
    // flow began inside the app (e.g. a UPI deep link), which a manual-record
    // caller cannot credibly self-assert. These manual paths are always false; a
    // future in-app payment endpoint is the only thing that sets it true.
    private static final boolean MANUALLY_RECORDED = false;

    /** "I paid {toUser}." Caller is the debtor. */
    @Transactional
    public SettlementResponseDTO createPaid(User principal, UUID groupId, PaidSettlementRequestDTO request) {
        return create(principal, groupId,
                principal.getId(), request.getToUserId(),
                request.getCurrencyCode(), request.getAmount(),
                request.getMethod(), request.getNote(), MANUALLY_RECORDED);
    }

    /** "{fromUser} paid me." Caller is the creditor. */
    @Transactional
    public SettlementResponseDTO createReceived(User principal, UUID groupId, ReceivedSettlementRequestDTO request) {
        return create(principal, groupId,
                request.getFromUserId(), principal.getId(),
                request.getCurrencyCode(), request.getAmount(),
                request.getMethod(), request.getNote(), MANUALLY_RECORDED);
    }

    /** Explicit payer + payee. Caller may be a third party (policy-gated). */
    @Transactional
    public SettlementResponseDTO record(User principal, UUID groupId, RecordSettlementRequestDTO request) {
        return create(principal, groupId,
                request.getFromUserId(), request.getToUserId(),
                request.getCurrencyCode(), request.getAmount(),
                request.getMethod(), request.getNote(), MANUALLY_RECORDED);
    }

    // ---------------------------------------------------------------------
    // Core creation
    // ---------------------------------------------------------------------

    private SettlementResponseDTO create(User principal,
                                         UUID groupId,
                                         UUID fromUserId,
                                         UUID toUserId,
                                         String currencyCode,
                                         BigDecimal amount,
                                         SettlementMethod method,
                                         String note,
                                         boolean initiatedViaApp) {

        if (fromUserId.equals(toUserId)) {
            throw new InvalidSettlementException("A settlement must be between two different members.");
        }

        Group group = groupRepository.findById(groupId).orElseThrow(GroupNotFoundException::new);
        Currency currency = currencyRepository.findByCode(currencyCode)
                .orElseThrow(() -> new InvalidCurrencyException(currencyCode));

        // Both parties must be active members of the group.
        requireActiveMember(groupId, fromUserId);
        requireActiveMember(groupId, toUserId);

        // Authorise the caller and decide the initial state in one step.
        SettlementStatus initialStatus = authoriseAndResolveStatus(principal, group, fromUserId, toUserId);

        long amountMinor = MoneyUtils.toMinor(amount, currency);

        // Proposal-time validation: the debtor must currently owe at least this much.
        // Runs for every path (proposed AND auto/creditor-confirmed) — it is the one
        // point where a "current debt" exists to check against. Confirmation never
        // re-checks, because by then the payment is a historical fact.
        validateAgainstOutstandingDebt(groupId, fromUserId, currency.getId(), amountMinor);

        Settlement settlement = Settlement.builder()
                .group(group)
                .fromUser(userRepository.getReferenceById(fromUserId))
                .toUser(userRepository.getReferenceById(toUserId))
                .currency(currency)
                .amountMinor(amountMinor)
                .status(SettlementStatus.PROPOSED)   // may be promoted below
                .method(method)
                .note(note)
                .initiatedViaApp(initiatedViaApp)
                .build();

        Settlement saved = settlementRepository.save(settlement);

        if (initialStatus == SettlementStatus.CONFIRMED) {
            applyConfirmation(saved);
        }

        return SettlementResponseDTO.from(saved);
    }

    /**
     * Verifies the caller may create this settlement and returns the state it
     * should land in.
     * <p>
     * Caller is a party:
     *   - payer (debtor)   -> PROPOSED (unless the group auto-confirms)
     *   - payee (creditor) -> CONFIRMED (the creditor is asserting receipt)
     * Caller is a third party:
     *   - allowed only per {@link ThirdPartySettlementPolicy}; lands PROPOSED
     *     (a non-party cannot assert receipt) unless the group auto-confirms.
     * autoSettle always promotes to CONFIRMED regardless of who created it.
     */
    private SettlementStatus authoriseAndResolveStatus(User principal, Group group, UUID fromUserId, UUID toUserId) {
        UUID actorId = principal.getId();
        GroupPreferences prefs = GroupPreferences.of(group);
        boolean autoSettle = prefs.getBoolean("autoSettle", false);

        boolean actorIsCreditor = actorId.equals(toUserId);
        boolean actorIsDebtor = actorId.equals(fromUserId);

        if (actorIsCreditor) {
            return SettlementStatus.CONFIRMED;          // "I received it" — self-confirming
        }
        if (actorIsDebtor) {
            return autoSettle ? SettlementStatus.CONFIRMED : SettlementStatus.PROPOSED;
        }

        // Third party: neither payer nor payee. Governed by group policy.
        requireThirdPartyRecordingAllowed(group.getId(), actorId, prefs);
        return autoSettle ? SettlementStatus.CONFIRMED : SettlementStatus.PROPOSED;
    }

    private void requireThirdPartyRecordingAllowed(UUID groupId, UUID actorId, GroupPreferences prefs) {
        // The recorder must themselves be an active member before any policy check.
        GroupMembership actor = membershipRepository
                .findByGroupIdAndUserIdAndStatus(groupId, actorId, MembershipStatus.ACTIVE)
                .orElseThrow(NotAMemberException::new);

        ThirdPartySettlementPolicy policy = prefs.getEnum(
                "thirdPartySettlementPolicy", ThirdPartySettlementPolicy.class,
                ThirdPartySettlementPolicy.DISABLED);

        switch (policy) {
            case DISABLED -> throw new SettlementForbiddenException(
                    "This group does not allow recording settlements on behalf of other members.");
            case ADMIN_ONLY -> {
                if (actor.getRole() != GroupRole.ADMIN) {
                    throw new SettlementForbiddenException(
                            "Only group admins may record settlements on behalf of other members.");
                }
            }
            case ALL_MEMBERS -> { /* any active member may record */ }
        }
    }

    /**
     * Loose, net-based debt check. Balances are stored per-user-net, not pairwise,
     * so "the debt" is the debtor's own negative net in this currency. A positive
     * or absent net means no outstanding debt to settle.
     */
    private void validateAgainstOutstandingDebt(UUID groupId, UUID fromUserId, UUID currencyId, long amountMinor) {
        long net = balanceRepository
                .findByGroupIdAndUserIdAndCurrencyId(groupId, fromUserId, currencyId)
                .map(Balance::getNetAmountMinor)
                .orElse(0L);

        long outstandingDebt = net < 0 ? -net : 0L;

        if (outstandingDebt == 0L) {
            throw new InvalidSettlementException("Nothing to settle.");
        }
        if (amountMinor > outstandingDebt) {
            throw new InvalidSettlementException("Settlement amount exceeds the outstanding debt.");
        }
    }

    // ---------------------------------------------------------------------
    // Lifecycle transitions
    // ---------------------------------------------------------------------

    /** Creditor accepts a proposed settlement. Applies balances. */
    @Transactional
    public SettlementResponseDTO confirm(User principal, UUID groupId, UUID settlementId) {
        Settlement settlement = loadForGroup(settlementId, groupId);
        requireProposed(settlement, "confirmed");

        if (!principal.getId().equals(settlement.getToUser().getId())) {
            throw new SettlementForbiddenException("Only the recipient can confirm this settlement.");
        }

        applyConfirmation(settlement);
        return SettlementResponseDTO.from(settlement);
    }

    /** Creditor declines a proposed settlement. No balance change. */
    @Transactional
    public SettlementResponseDTO reject(User principal, UUID groupId, UUID settlementId) {
        Settlement settlement = loadForGroup(settlementId, groupId);
        requireProposed(settlement, "rejected");

        if (!principal.getId().equals(settlement.getToUser().getId())) {
            throw new SettlementForbiddenException("Only the recipient can reject this settlement.");
        }

        settlement.setStatus(SettlementStatus.REJECTED);
        return SettlementResponseDTO.from(settlement);
    }

    /** Proposer (debtor) withdraws a proposed settlement. No balance change. */
    @Transactional
    public SettlementResponseDTO cancel(User principal, UUID groupId, UUID settlementId) {
        Settlement settlement = loadForGroup(settlementId, groupId);
        requireProposed(settlement, "cancelled");

        if (!principal.getId().equals(settlement.getFromUser().getId())) {
            throw new SettlementForbiddenException("Only the payer can cancel this settlement.");
        }

        settlement.setStatus(SettlementStatus.CANCELLED);
        return SettlementResponseDTO.from(settlement);
    }

    /**
     * Promote a settlement to CONFIRMED and move balances. Shared by the create
     * paths (creditor-created / auto-settle) and the standalone confirm endpoint.
     * The @Version guard on Settlement makes a concurrent double-confirm fail
     * rather than apply deltas twice. Balance movement runs in the same
     * transaction (BalanceUpdateService joins the caller's REQUIRED tx).
     */
    private void applyConfirmation(Settlement settlement) {
        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setConfirmedAt(Instant.now());
        balanceUpdateService.applyDeltas(BalanceUpdateCommandFactory.from(settlement));
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SettlementResponseDTO get(User principal, UUID groupId, UUID settlementId) {
        requireActiveMember(groupId, principal.getId());
        Settlement settlement = loadForGroup(settlementId, groupId);
        return SettlementResponseDTO.from(settlement);
    }

    /**
     * Group-scoped settlement history, newest first, optionally filtered by
     * status. Returns entities (not DTOs) so the controller maps via PagedResponse;
     * the repository @EntityGraph fetches currency, so mapping outside this tx is
     * safe (only currency is read eagerly by the response DTO; from/to are ids off
     * lazy FK proxies).
     */
    @Transactional(readOnly = true)
    public Page<Settlement> list(User principal, UUID groupId, SettlementStatus status, Pageable pageable) {
        requireActiveMember(groupId, principal.getId());
        return (status == null)
                ? settlementRepository.findByGroupId(groupId, pageable)
                : settlementRepository.findByGroupIdAndStatus(groupId, status, pageable);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Settlement loadForGroup(UUID settlementId, UUID groupId) {
        return settlementRepository.findByIdAndGroupId(settlementId, groupId)
                .orElseThrow(SettlementNotFoundException::new);
    }

    private void requireProposed(Settlement settlement, String action) {
        if (settlement.getStatus() != SettlementStatus.PROPOSED) {
            throw new SettlementStateException(
                    "Only a proposed settlement can be " + action + "; this one is "
                            + settlement.getStatus() + ".");
        }
    }

    private void requireActiveMember(UUID groupId, UUID userId) {
        if (!membershipRepository.existsByGroupIdAndUserIdAndStatus(groupId, userId, MembershipStatus.ACTIVE)) {
            throw new NotAMemberException();
        }
    }
}
