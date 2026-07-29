package com.vyay.core.dto.response.balance;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.balance.UserBalanceTotal;
import com.vyay.core.entity.reference.Currency;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * One row of GET /me/balances: the authenticated user's aggregate position in a
 * single currency, across all groups.
 *
 * owed  = money owed to the user (positive), owing = money the user owes
 * (positive magnitude), net = owed - owing. Minor units are converted to major
 * using the currency's scale.
 */
@Getter
@Builder
public class UserBalanceResponseDTO {

    private String currencyCode;
    private BigDecimal owed;
    private BigDecimal owing;
    private BigDecimal net;

    public static UserBalanceResponseDTO from(UserBalanceTotal total) {
        Currency currency = total.getCurrency();
        return UserBalanceResponseDTO.builder()
                .currencyCode(currency.getCode())
                .owed(MoneyUtils.toMajor(total.getTotalOwedMinor(), currency))
                .owing(MoneyUtils.toMajor(total.getTotalOwingMinor(), currency))
                .net(MoneyUtils.toMajor(total.netMinor(), currency))
                .build();
    }
}
