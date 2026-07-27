package com.vyay.core.services.expense;

import com.vyay.core.entity.User;
import com.vyay.core.entity.expense.Expense;
import com.vyay.core.entity.expense.ExpensePayer;
import com.vyay.core.entity.expense.ExpenseShare;
import com.vyay.core.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ExpenseLineFactory {

    private final UserRepository userRepository;

    public ExpenseLineFactory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public ExpensePayer payer(Expense expense, User user, long amountMinor) {
        return ExpensePayer.builder()
                .expense(expense)
                .user(userRepository.getReferenceById(user.getId()))
                .amountPaidMinor(amountMinor)
                .build();
    }

    public ExpenseShare share(Expense expense, User user, long owedMinor, BigDecimal pct, Integer weight) {
        return ExpenseShare.builder()
                .expense(expense)
                .user(userRepository.getReferenceById(user.getId()))
                .owedAmountMinor(owedMinor)
                .percentage(pct)
                .shareWeight(weight)
                .build();
    }
}