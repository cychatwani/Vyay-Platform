package com.vyay.events.v1;

/**
 * A user-lifecycle event, which belongs to no group — {@code envelope().groupId()}
 * is null for these and only these.
 *
 * The one place display data is allowed. These feed a server-side user reference
 * projection used for rendering email digests, notification copy and AI summaries;
 * carrying a name and an avatar is their entire purpose. The activity feed API
 * never reads that projection, and no {@link GroupEvent} carries identity — a
 * client resolves who did what from its own member store.
 */
public sealed interface UserEvent extends DomainEvent permits UserCreated, UserUpdated {
}
