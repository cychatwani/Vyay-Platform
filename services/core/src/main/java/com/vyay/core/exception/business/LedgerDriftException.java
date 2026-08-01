// LedgerDriftException.java
package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

/**
 * A group's per-currency balances did not net to zero — a data-integrity fault,
 * not a client error. Thrown when settlement-plan generation finds drift. The
 * client message is deliberately generic; the service logs the group/currency
 * before throwing so the real cause is traceable.
 */
public class LedgerDriftException extends BusinessException {
    public LedgerDriftException() {
        super("Something went wrong. Please try again later.",
                "ERR_LEDGER_DRIFT", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
