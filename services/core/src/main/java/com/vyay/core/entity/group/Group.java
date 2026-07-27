package com.vyay.core.entity.group;

import com.vyay.core.entity.User;
import com.vyay.core.entity.base.SoftDeletableEntity;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.GroupType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "groups")
@SQLDelete(sql = "update groups set deleted_at = now() where id = ?")
@SQLRestriction("deleted_at is null")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Group extends SoftDeletableEntity {

    @Column(nullable = false)
    private String name;

    private String description; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_currency", nullable = false)
    private Currency defaultCurrency;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "type", columnDefinition = "group_type", nullable = false)
    private GroupType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default
    private Integer memberCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<GroupPreference> preferences = new ArrayList<>();
}