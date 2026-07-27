package com.vyay.core.entity.settlement;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.SoftDeletableEntity;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.SettlementMethod;
import com.vyay.core.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Entity
@Table(name = "settlements")
@SQLDelete(sql = "update settlements set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Settlement extends SoftDeletableEntity {

    // Settlements clear GROUP balances, so always group-scoped (never personal).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // Who pays (the debtor settling up).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    // Who receives (the creditor being paid back).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    // Amount transferred, in MINOR units. Always positive.
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "settlement_status", nullable = false)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PROPOSED;

    // The channel money moved through. Null until known.
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "method", columnDefinition = "settlement_method")
    private SettlementMethod method;

    // Indicates whether the payment was initiated from within the app
// (e.g. via a UPI deep link) rather than being manually recorded.
    @Column(name = "initiated_via_app", nullable = false)
    @Builder.Default
    private boolean initiatedViaApp = false ;

    private String note;  // nullable memo

    // Domain timestamp, NOT an audit field — kept here, not in the base.
    private Instant confirmedAt;  // nullable
}