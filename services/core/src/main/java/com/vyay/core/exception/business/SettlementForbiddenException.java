// SettlementForbiddenException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class SettlementForbiddenException extends BusinessException {
    public SettlementForbiddenException(String message) {
        super(message, "ERR_SETTLEMENT_FORBIDDEN", HttpStatus.FORBIDDEN);
    }
}
