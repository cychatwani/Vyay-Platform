package com.vyay.core.entity.reference;

import com.vyay.core.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "languages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_language_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Language extends BaseEntity {

    @Column(nullable = false, length = 5, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String nativeName;
}