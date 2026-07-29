package com.vyay.core.controllers;

import com.vyay.core.dto.response.balance.UserBalanceResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.services.balance.BalanceQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me")
public class BalanceController {

    private final BalanceQueryService balanceQueryService;

    public BalanceController(BalanceQueryService balanceQueryService) {
        this.balanceQueryService = balanceQueryService;
    }

    /** Per-currency owed / owing / net for the authenticated user, across all groups. */
    @GetMapping("/balances")
    public ResponseEntity<ApiResponse<List<UserBalanceResponseDTO>>> getMyBalances(
            @AuthenticationPrincipal User user) {
        List<UserBalanceResponseDTO> balances = balanceQueryService.getMyBalances(user.getId());
        return ResponseEntity.ok(ApiResponse.success(balances));
    }
}
