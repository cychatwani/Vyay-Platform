package com.vyay.core.controllers;

import com.vyay.core.dto.response.settlement.SettlementPlanResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.services.settlement.plan.SettlementPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/settlement-plan")
public class SettlementPlanController {

    private final SettlementPlanService settlementPlanService;

    public SettlementPlanController(SettlementPlanService settlementPlanService) {
        this.settlementPlanService = settlementPlanService;
    }

    /**
     * Generate the group's live settlement plan for a currency. Doubles as
     * regenerate — any existing live plan is superseded — so this always creates a
     * new plan and always answers 201.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SettlementPlanResponseDTO>> generate(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @RequestParam String currencyCode) {
        SettlementPlanResponseDTO plan = settlementPlanService.generate(user, groupId, currencyCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(plan, "Settlement plan generated."));
    }

    /** The current live plan (ACTIVE or STALE) for a currency. */
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementPlanResponseDTO>> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @RequestParam String currencyCode) {
        return ResponseEntity.ok(ApiResponse.success(settlementPlanService.get(user, groupId, currencyCode)));
    }

    /** Cancel the live plan — admins only. */
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @RequestParam String currencyCode) {
        settlementPlanService.cancel(user, groupId, currencyCode);
        return ResponseEntity.ok(ApiResponse.success(null, "Settlement plan cancelled."));
    }
}
