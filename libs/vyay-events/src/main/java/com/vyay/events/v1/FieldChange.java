package com.vyay.events.v1;

/**
 * One field altered by an edit: what it was, what it became.
 *
 * Carried as a list so that one user action is one event and one feed row.
 * Renaming a group and rewriting its description in a single save is one thing
 * the user did, and splitting it into two events would render as two entries that
 * are indistinguishable from two separate edits a minute apart.
 *
 * @param field    the domain field name, e.g. {@code "name"}, {@code "description"}.
 *                 A String and not an enum, deliberately: making a field editable
 *                 later must not emit a constant that older consumers cannot
 *                 deserialise. Closed domain vocabularies like {@link SplitType}
 *                 are enums precisely because they do not grow this way.
 * @param oldValue previous value, rendered in its canonical string form. Null when
 *                 the field was previously unset.
 * @param newValue new value, same rendering. Null when the field was cleared.
 */
public record FieldChange(String field, String oldValue, String newValue) {
}
