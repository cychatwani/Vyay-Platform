package com.vyay.core.repository;

import com.vyay.core.entity.expense.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    @Query("SELECT e FROM Expense e WHERE e.id = :id AND e.deletedAt IS NULL")
    Optional<Expense> findActiveById(@Param("id") UUID id);
}
