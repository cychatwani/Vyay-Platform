// InvalidSettlementException.java
package com.vyay.core.exception.business;
import org.springframework.http.HttpStatus;
public class InvalidSettlementException extends BusinessException {
    public InvalidSettlementException(String message) {
        super(message, "ERR_INVALID_SETTLEMENT", HttpStatus.BAD_REQUEST);
    }
}
