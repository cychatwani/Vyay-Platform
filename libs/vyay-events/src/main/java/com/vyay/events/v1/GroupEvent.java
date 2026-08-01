package com.vyay.events.v1;

import java.util.UUID;

/**
 * An event that happened inside a group, and therefore always has a
 * {@code groupId}.
 *
 * Everything the activity feed renders is one of these. The split from
 * {@link UserEvent} is what lets a feed consumer read {@link #groupId()} without a
 * null check and without a comment promising it is safe.
 */
public sealed interface GroupEvent extends DomainEvent permits
        ExpenseCreated, ExpenseUpdated, ExpenseDeleted,
        SettlementProposed, SettlementConfirmed, SettlementRejected, SettlementCancelled,
        MemberJoined, MemberLeft, MemberRemoved, MemberRoleChanged,
        GroupCreated, GroupDetailsUpdated, GroupAvatarUpdated,
        SettlementPlanGenerated, SettlementPlanCompleted {

    /** The group this happened in. Never null for a {@code GroupEvent}. */
    default UUID groupId() {
        return envelope().groupId();
    }
}
