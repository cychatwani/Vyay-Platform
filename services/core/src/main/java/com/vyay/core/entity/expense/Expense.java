    package com.vyay.core.entity.expense;

    import com.vyay.core.entity.User;
    import com.vyay.core.entity.base.SoftDeletableEntity;
    import com.vyay.core.entity.group.Group;
    import com.vyay.core.entity.reference.Currency;
    import com.vyay.core.enums.SplitType;
    import jakarta.persistence.*;
    import lombok.*;
    import lombok.experimental.SuperBuilder;
    import org.hibernate.annotations.SQLRestriction;
    import org.hibernate.annotations.JdbcType;
    import org.hibernate.dialect.PostgreSQLEnumJdbcType;

    import java.time.Instant;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "expenses")
    @SQLRestriction("deleted_at is null")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @SuperBuilder
    public class Expense extends SoftDeletableEntity {

    @Column(nullable = false)
    private String description;

    // Stored in MINOR units of `currency` (e.g. paise for INR). This is MONEY, not
    // an id —
    // it stays Long. Major value is derived in the response DTO as minor /
    // 10^decimalPlaces.
    @Column(name = "total_amount_minor", nullable = false)
    private Long totalAmountMinor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    // Nullable group_id == a PERSONAL (unsplit) expense.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // Who recorded the expense (the actor) — not necessarily who paid.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // Null == not split (personal). Set when split among members.
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "split_type", columnDefinition = "split_type")
    private SplitType splitType;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpensePayer> payers = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseShare> shares = new ArrayList<>();

    // When the expense actually occurred — distinct from createdAt (when the row
    // was recorded).
    @Column(nullable = false)
    private Instant expenseDate;

    private String notes; // nullable

    // Keeps its OWN @PrePersist for the expenseDate default. id-gen and timestamps
    // come from
    // the base; JPA fires assignId -> onCreate -> applyDefaults, superclass-first.
    @PrePersist
    private void applyDefaults() {
        if (expenseDate == null) {
            expenseDate = Instant.now();
        }
    }
    }