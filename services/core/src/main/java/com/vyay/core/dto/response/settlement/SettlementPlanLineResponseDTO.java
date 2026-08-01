package com.vyay.core.dto.response.settlement;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.SettlementPlanLineStatus;
import com.vyay.core.repository.projection.PlanLineProgressView;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line of a settlement plan's read model: fromUser pays toUser, with derived
 * progress. All money is in major units.
 *
 * fulfilled = CONFIRMED settlements applied to this line (drives {@code status}).
 * pending   = PROPOSED settlements applied to this line (in-flight, inert).
 * remaining = amount - fulfilled - pending — "still to send", the actionable
 *             figure. (The truly-unsettled amount is amount - fulfilled.)
 * appended  = this line was added after the plan was generated. Read straight off
 *             the line, which stores it.
 *
 * A carried line — one written for a settlement that was already in flight when
 * the plan was generated — reads amount = pending = the settlement's amount, so
 * remaining is 0: the plan shows the money as on its way and never asks for it.
 */
@Getter
@Builder
public class SettlementPlanLineResponseDTO {

    private UUID fromUserId;
    private UUID toUserId;

    private BigDecimal amount;
    private BigDecimal fulfilled;
    private BigDecimal pending;
    private BigDecimal remaining;

    private SettlementPlanLineStatus status;
    private boolean appended;

    public static SettlementPlanLineResponseDTO from(PlanLineProgressView v, Currency currency) {
        long amount = v.getAmountMinor();
        long fulfilled = v.getFulfilledMinor();
        long pending = v.getPendingMinor();

        SettlementPlanLineStatus status =
                fulfilled <= 0 ? SettlementPlanLineStatus.PENDING
                        : fulfilled >= amount ? SettlementPlanLineStatus.COMPLETED
                        : SettlementPlanLineStatus.PARTIALLY_FULFILLED;

        return SettlementPlanLineResponseDTO.builder()
                .fromUserId(v.getFromUserId())
                .toUserId(v.getToUserId())
                .amount(MoneyUtils.toMajor(amount, currency))
                .fulfilled(MoneyUtils.toMajor(fulfilled, currency))
                .pending(MoneyUtils.toMajor(pending, currency))
                .remaining(MoneyUtils.toMajor(amount - fulfilled - pending, currency))
                .status(status)
                .appended(v.getAppended())
                .build();
    }
}
