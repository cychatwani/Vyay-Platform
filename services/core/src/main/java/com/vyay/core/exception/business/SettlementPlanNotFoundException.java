// SettlementPlanNotFoundException.java
package com.vyay.core.exception.business;

import org.springframework.http.HttpStatus;

public class SettlementPlanNotFoundException extends BusinessException {
    public SettlementPlanNotFoundException() {
        super("No active settlement plan for this group and currency.",
                "ERR_SETTLEMENT_PLAN_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
