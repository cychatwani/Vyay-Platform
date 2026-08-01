package com.vyay.events.v1;

/**
 * A group's avatar was set, replaced, or removed.
 *
 * NO PRODUCER IN CORE YET, and no avatar column exists on the group either — this
 * is the furthest ahead of the domain of the three speculative types. Defined for
 * the reason on {@link MemberRoleChanged}.
 *
 * Kept separate from {@link GroupDetailsUpdated} rather than folded in as another
 * changed field: an image URL is not a value a feed row should print, and the two
 * have different lifetimes — an avatar URL can be re-issued by storage without the
 * group being edited at all.
 *
 * @param avatarUrl where the image now lives, or NULL when the avatar was removed.
 *                  Null is the removal signal; there is no separate event for it.
 */
public record GroupAvatarUpdated(
        EventEnvelope envelope,
        String avatarUrl) implements GroupEvent {

    public static final String TYPE = "group.avatar_updated.v1";

    @Override
    public String eventType() {
        return TYPE;
    }
}
