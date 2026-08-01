package com.vyay.events.v1;

import java.util.List;
import java.util.UUID;

/**
 * A user's profile changed.
 *
 * Not group-scoped: {@code envelope().groupId()} is null. Carries display data for
 * the reason set out on {@link UserCreated} — together they are the identity feed.
 *
 * Full new state AND a change list, like {@link ExpenseUpdated}: the state lets a
 * reference projection overwrite its row without having seen the previous version,
 * which is what a consumer replaying from the start of the topic actually does,
 * while the change list lets a notification say what changed.
 *
 * @param userId            whose profile changed.
 * @param fullName          display name, after the change.
 * @param email             email address, after the change.
 * @param profilePictureUrl avatar location, after the change. Nullable.
 * @param changedFields     what this edit touched. Never empty.
 */
public record UserUpdated(
        EventEnvelope envelope,
        UUID userId,
        String fullName,
        String email,
        String profilePictureUrl,
        List<FieldChange> changedFields) implements UserEvent {

    public static final String TYPE = "user.updated.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
