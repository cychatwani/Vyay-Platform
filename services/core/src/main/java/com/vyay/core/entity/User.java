package com.vyay.core.entity;

import com.vyay.core.entity.base.SoftDeletableEntity;
import com.vyay.core.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class User extends SoftDeletableEntity {

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String fullName;  // denormalized from firstName + lastName

    private String profilePicture;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "auth_provider", columnDefinition = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(length = 100)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Keeps fullName in sync with firstName/lastName on insert and update.
     * The id (UUIDv7), created/updated timestamps, and deletedAt all come from
     * the base hierarchy. User intentionally declares NO @SQLDelete/@SQLRestriction:
     * deleted users stay queryable so historical FK references still resolve, and
     * "active user" is enforced in the auth queries (findByEmail ... and deleted_at is null).
     */
    @PrePersist
    @PreUpdate
    private void updateFullName() {
        this.fullName = (firstName != null ? firstName : "")
                + (lastName != null ? " " + lastName : "");
    }
}