package com.vyay.core.entity.group;

import java.util.HashMap;
import java.util.Map;

/**
 * Read-only view over a group's preference list, keyed for typed lookup.
 * <p>
 * Preferences persist as a polymorphic JSONB list ({@link GroupPreference}),
 * so callers should not scan that list by hand. This wrapper centralises key
 * lookup and type coercion, and — importantly — resolves a MISSING key to the
 * supplied default. Groups created before a given preference existed simply
 * have no entry for it; treating "absent" as "default" avoids a data backfill
 * over every group row. The trade-off: the effective default lives here, not
 * only in {@link GroupPreference#defaults()} (which seeds new groups). Keep the
 * two in sync if a default ever changes.
 */
public final class GroupPreferences {

    private final Map<String, Object> byKey;

    private GroupPreferences(Map<String, Object> byKey) {
        this.byKey = byKey;
    }

    public static GroupPreferences of(Group group) {
        Map<String, Object> map = new HashMap<>();
        if (group.getPreferences() != null) {
            for (GroupPreference p : group.getPreferences()) {
                map.put(p.key(), p.value());
            }
        }
        return new GroupPreferences(map);
    }

    /** Boolean preference, or {@code defaultValue} if absent or not a boolean. */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = byKey.get(key);
        return (value instanceof Boolean b) ? b : defaultValue;
    }

    /**
     * Enum preference stored as its {@code name()} string, or {@code defaultValue}
     * if absent or unrecognised. Unrecognised values fall back rather than throw:
     * a stale/garbage stored string should degrade to the safe default, not break
     * every request that reads the policy.
     */
    public <E extends Enum<E>> E getEnum(String key, Class<E> type, E defaultValue) {
        Object value = byKey.get(key);
        if (value instanceof String s) {
            try {
                return Enum.valueOf(type, s);
            } catch (IllegalArgumentException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}