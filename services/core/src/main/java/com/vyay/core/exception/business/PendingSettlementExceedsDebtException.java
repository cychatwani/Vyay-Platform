// PendingSettlementExceedsDebtException.java
package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * A settlement is in flight for more than its payer actually owes the group, so
 * no honest plan can be built around it.
 *
 * Reachable because settlement creation validates the payer's debt at PROPOSAL
 * time and nothing re-checks it afterwards: expenses and other confirmations move
 * that balance while the proposal sits unanswered. Carrying such a settlement
 * forward would leave the plan routing money BACK to the payer to compensate for
 * the overpayment — arithmetically sound, but nonsense to read.
 *
 * Refusing is better than planning around it. The settlement is the thing that is
 * wrong, and only its payer or payee can withdraw it, so the plan says so instead
 * of quietly absorbing it. 409: the request is well-formed, the group's state is
 * what conflicts, and it becomes valid again once the settlement is cancelled.
 */
public class PendingSettlementExceedsDebtException extends BusinessException {
    public PendingSettlementExceedsDebtException(UUID settlementId) {
        super("A pending settlement (" + settlementId + ") is for more than its payer currently owes "
                        + "in this group. Cancel or reject it, then generate the plan again.",
                "ERR_PENDING_SETTLEMENT_EXCEEDS_DEBT", HttpStatus.CONFLICT);
    }
}
