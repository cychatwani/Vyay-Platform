package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class OutstandingBalanceException extends BusinessException {
    public OutstandingBalanceException() {
        super("Settle your outstanding balances in this group before leaving.",
                "ERR_OUTSTANDING_BALANCE", HttpStatus.CONFLICT);
    }
}