package com.vyay.core.entity.balance;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.AuditableEntity;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_balance_group_user_currency",
                columnNames = {"group_id", "user_id", "currency_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Balance extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(name = "net_amount_minor", nullable = false)
    @Builder.Default
    private Long netAmountMinor = 0L;
}