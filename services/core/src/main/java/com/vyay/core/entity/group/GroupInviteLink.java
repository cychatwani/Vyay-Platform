package com.vyay.core.entity.group;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.SoftDeletableEntity;
import com.vyay.core.enums.InviteLinkType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;

@Entity
@Table(
        name = "group_invite_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_invite_link_code",
                columnNames = {"code"}
        )
)
@SQLDelete(sql = "update group_invite_links set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GroupInviteLink extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(nullable = false, updatable = false, length = 16)
    private String code;  // short, shareable, unique — service-generated

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", columnDefinition = "invite_link_type", nullable = false)
    private InviteLinkType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    private Instant expiresAt;   // nullable — null for PRIMARY

    private Integer maxUses;     // nullable — null means unlimited

    @Column(nullable = false)
    @Builder.Default
    private Integer useCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isActive = true;   // usability flag — distinct concern from soft-delete
}