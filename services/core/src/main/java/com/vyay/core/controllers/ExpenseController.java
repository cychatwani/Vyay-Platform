package com.vyay.core.controllers;

import com.vyay.core.dto.requests.expense.CreateExpenseRequestDTO;
import com.vyay.core.dto.response.expense.ExpenseResponseDTO;
import com.vyay.core.dto.wrapper.ApiResponse;
import com.vyay.core.entity.User;
import com.vyay.core.services.expense.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponseDTO>> createExpense(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateExpenseRequestDTO request) {
        ExpenseResponseDTO created = expenseService.createExpense(user, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Expense created"));
    }
}