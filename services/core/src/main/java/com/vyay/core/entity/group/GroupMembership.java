package com.vyay.core.entity.group;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.SoftDeletableEntity;
import com.vyay.core.enums.GroupRole;
import com.vyay.core.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Entity
@Table(
        name = "group_memberships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_membership_group_user",
                columnNames = {"group_id", "user_id"}
        )
)
@SQLDelete(sql = "update group_memberships set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GroupMembership extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "role", columnDefinition = "group_role", nullable = false)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "status", columnDefinition = "membership_status", nullable = false)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    // Start of the CURRENT stint — resets on rejoin. (createdAt = first-ever join, immutable.)
    @Column(nullable = false)
    @Builder.Default
    private Instant activeSince  = Instant.now();

    private Instant leftAt;  // nullable; set on leave/remove, cleared on rejoin
}