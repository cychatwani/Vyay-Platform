package com.vyay.core.entity;

import com.vyay.core.entity.reference.Currency;
import com.vyay.core.entity.reference.Language;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;  // shared PK, derived from User via @MapsId — NOT generated here

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency", nullable = false)
    private Currency defaultCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language", nullable = false)
    private Language language;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private String preferences = "[]";

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }
}