// SettlementPlanConflictException.java
package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

/**
 * A concurrent write moved the live plan out from under a generate — a parallel
 * regenerate, cancel or completion landed first. Not the "a plan already exists"
 * case: generate supersedes an existing plan rather than refusing. Retrying after
 * a refresh is the expected client response, hence 409 rather than a 500.
 */
public class SettlementPlanConflictException extends BusinessException {
    public SettlementPlanConflictException() {
        super("The settlement plan changed while this one was being generated. Please refresh and try again.",
                "ERR_SETTLEMENT_PLAN_CONFLICT", HttpStatus.CONFLICT);
    }
}
