// SettlementStateException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class SettlementStateException extends BusinessException {
    public SettlementStateException(String message) {
        super(message, "ERR_SETTLEMENT_STATE", HttpStatus.CONFLICT);
    }
}
