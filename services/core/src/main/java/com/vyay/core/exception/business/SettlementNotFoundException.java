// SettlementNotFoundException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class SettlementNotFoundException extends BusinessException {
    public SettlementNotFoundException() {
        super("Settlement not found.", "ERR_SETTLEMENT_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
