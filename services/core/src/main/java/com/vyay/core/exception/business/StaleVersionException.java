package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

/**
 * Raised when a load → modify → save edit detects that the entity's version
 * has been bumped by a concurrent edit since the client read it. The client
 * should re-fetch, present the latest state, and retry if still relevant.
 *
 * Translated from Spring's ObjectOptimisticLockingFailureException by
 * GlobalExceptionHandler.
 */
public class StaleVersionException extends BusinessException {
    public StaleVersionException() {
        super("The resource was modified by someone else. Please reload and try again.",
                "ERR_STALE_VERSION",
                HttpStatus.CONFLICT);
    }
}