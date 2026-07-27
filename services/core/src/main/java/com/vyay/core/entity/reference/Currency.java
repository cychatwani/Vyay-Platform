package com.vyay.core.entity.reference;

import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "currencies",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_currency_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Currency extends BaseEntity {

    @Column(nullable = false, length = 3, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 5)
    private String symbol;

    @Column(nullable = false)
    private int decimalPlaces;
}