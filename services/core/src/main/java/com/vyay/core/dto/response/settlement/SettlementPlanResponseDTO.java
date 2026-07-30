package com.vyay.core.dto.response.settlement;

import com.vyay.core.enums.SettlementPlanStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read model for a settlement plan: the aggregate plus its lines with derived
 * progress. Assembled in the service (multi-source: plan + progress projection +
 * currency), so no static factory here.
 */
@Getter
@Builder
public class SettlementPlanResponseDTO {

    private UUID planId;
    private UUID groupId;
    private String currencyCode;
    private String currencySymbol;
    private SettlementPlanStatus status;
    private Instant createdAt;
    private List<SettlementPlanLineResponseDTO> lines;
}
