package com.vyay.events.v1;

import java.util.UUID;

/**
 * A user account was created.
 *
 * Not group-scoped: {@code envelope().groupId()} is null, and
 * {@code envelope().actorUserId()} is the new user themselves.
 *
 * <h2>Why this one carries display data</h2>
 * Every other event in the catalogue carries ids and no identity, because the
 * mobile client resolves names and avatars from its own member store. This pair
 * ({@code UserCreated} / {@link UserUpdated}) is the exception, and the reason is
 * that something has to be the source of that identity somewhere else: Insights
 * keeps a user reference projection fed by exactly these two events, used for
 * SERVER-side rendering — email digests, notification copy, AI summaries — where
 * there is no client store to read from. The activity feed API never touches that
 * projection. Nothing else here should carry a name.
 *
 * @param userId            the new user's id, repeated from the envelope's actor
 *                          because the subject of the event is a user, and a
 *                          consumer projecting users should read a subject field
 *                          rather than infer it from the actor.
 * @param fullName          display name.
 * @param email             the account's email address.
 * @param profilePictureUrl avatar location. Nullable.
 */
public record UserCreated(
        EventEnvelope envelope,
        UUID userId,
        String fullName,
        String email,
        String profilePictureUrl) implements UserEvent {

    public static final String TYPE = "user.created.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
