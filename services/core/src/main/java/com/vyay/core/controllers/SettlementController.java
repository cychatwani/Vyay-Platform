package com.vyay.core.controllers;

import com.vyay.core.dto.requests.settlement.PaidSettlementRequestDTO;
import com.vyay.core.dto.requests.settlement.ReceivedSettlementRequestDTO;
import com.vyay.core.dto.requests.settlement.RecordSettlementRequestDTO;
    import com.vyay.core.dto.response.settlement.SettlementResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.dto.wrapper.PagedResponse;
import com.vyay.core.entity.User;
import com.vyay.core.entity.settlement.Settlement;
import com.vyay.core.enums.SettlementStatus;
import com.vyay.core.services.settlement.SettlementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Settlements are always group-scoped, so every route hangs off the group.
 * <p>
 * Three creation entry points funnel into the service's single create path:
 *   /paid     — "I paid {counterparty}"          (principal is the debtor)
 *   /received — "{counterparty} paid me"          (principal is the creditor)
 *   /record   — "{fromUser} paid {toUser}"        (explicit; principal may be a
 *               third party, gated by the group's third-party settlement policy)
 * <p>
 * Resulting state (PROPOSED vs CONFIRMED), debt validation, and authorization
 * all live in the service — the controller only shapes HTTP.
 */
@RestController
@RequestMapping("/groups/{groupId}/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/paid")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> createPaid(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody PaidSettlementRequestDTO request) {
        SettlementResponseDTO created = settlementService.createPaid(user, groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Settlement recorded"));
    }

    @PostMapping("/received")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> createReceived(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody ReceivedSettlementRequestDTO request) {
        SettlementResponseDTO created = settlementService.createReceived(user, groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Settlement recorded"));
    }

    @PostMapping("/record")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> record(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @Valid @RequestBody RecordSettlementRequestDTO request) {
        SettlementResponseDTO created = settlementService.record(user, groupId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Settlement recorded"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SettlementResponseDTO>>> list(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlements = settlementService.list(user, groupId, status, pageable);
        return ResponseEntity.ok(
                ApiResponse.success(PagedResponse.from(settlements, SettlementResponseDTO::from)));
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> get(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.get(user, groupId, settlementId)));
    }

    @PostMapping("/{settlementId}/confirm")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> confirm(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.confirm(user, groupId, settlementId),
                        "Settlement confirmed"));
    }

    @PostMapping("/{settlementId}/reject")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> reject(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.reject(user, groupId, settlementId),
                        "Settlement rejected"));
    }

    @PostMapping("/{settlementId}/cancel")
    public ResponseEntity<ApiResponse<SettlementResponseDTO>> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable UUID groupId,
            @PathVariable UUID settlementId) {
        return ResponseEntity.ok(
                ApiResponse.success(settlementService.cancel(user, groupId, settlementId),
                        "Settlement cancelled"));
    }
}
