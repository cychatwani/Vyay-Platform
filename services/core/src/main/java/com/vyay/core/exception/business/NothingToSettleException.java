// NothingToSettleException.java
package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

/**
 * A group is already square for this currency, so there is no plan to generate.
 * Distinct from {@link InvalidSettlementException} on purpose: this fires at the
 * moment the group SUCCEEDS at settling up, and the client should render it as
 * "all settled" rather than as a failure. The errorCode is what carries that
 * distinction — the status stays 400 so existing callers see no contract change.
 *
 * Kept as an exception rather than a null-plan response shape: generate either
 * produces a plan or it does not, and the caller already branches on errorCode.
 */
public class NothingToSettleException extends BusinessException {
    public NothingToSettleException() {
        super("There is nothing to settle in this group for this currency.",
                "ERR_NOTHING_TO_SETTLE", HttpStatus.BAD_REQUEST);
    }
}
