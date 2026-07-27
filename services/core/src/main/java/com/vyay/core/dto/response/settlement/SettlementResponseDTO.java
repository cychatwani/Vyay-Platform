package com.vyay.core.dto.response.settlement;

import com.vyay.core.common.utils.MoneyUtils;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.SettlementMethod;
import com.vyay.core.enums.SettlementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Canonical read model for a settlement. Exposes the two parties by id only:
 * fromUser/toUser/group are lazy associations and .getId() resolves off the FK
 * without initialising the proxy. Currency IS read (code + symbol), so callers
 * that build this from a store-fetched settlement must have the currency loaded
 * (the create path already holds a resolved Currency; the read path JOIN FETCHes it).
 */
@Getter
@Builder
public class SettlementResponseDTO {

    private UUID settlementId;
    private UUID groupId;
    private UUID fromUserId;
    private UUID toUserId;

    private Long amountMinor;
    private BigDecimal amount;
    private String currencyCode;
    private String currencySymbol;

    private SettlementStatus status;
    private SettlementMethod method;
    private boolean initiatedViaApp;
    private String note;

    private Instant confirmedAt;
    private Instant createdAt;

    public static SettlementResponseDTO from(Settlement s) {
        Currency c = s.getCurrency();
        return SettlementResponseDTO.builder()
                .settlementId(s.getId())
                .groupId(s.getGroup().getId())
                .fromUserId(s.getFromUser().getId())
                .toUserId(s.getToUser().getId())
                .amountMinor(s.getAmountMinor())
                .amount(MoneyUtils.toMajor(s.getAmountMinor(), c))
                .currencyCode(c.getCode())
                .currencySymbol(c.getSymbol())
                .status(s.getStatus())
                .method(s.getMethod())
                .initiatedViaApp(s.isInitiatedViaApp())
                .note(s.getNote())
                .confirmedAt(s.getConfirmedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}