package com.vyay.events.v1;

/**
 * How an expense total was divided among its participants.
 *
 * A deliberate copy of the Core domain enum rather than a shared type. Core cannot
 * be a dependency here — the arrow points the other way — and its enum is bound to
 * a Postgres enum type through JPA, which is a persistence concern this contract
 * must be free to diverge from. The cost is one mapping at publish time; the
 * benefit is that renaming a constant in the domain becomes a compile error at the
 * boundary instead of a silent change to the wire format.
 */
public enum SplitType {
    /** Total divided evenly; the leftover minor unit is allocated deterministically. */
    EQUAL,
    /** Each share's amount given directly; they must sum to the total. */
    EXACT,
    /** Each share carries a percentage; the amount is resolved from it. */
    PERCENTAGE,
    /** Each share carries a weight; the amount is resolved pro-rata. */
    SHARES
}
