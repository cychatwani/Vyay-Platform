/**
 * Version 1 of the Vyay domain event contract.
 *
 * <h2>What these are</h2>
 * Plain Java records describing facts that already happened in the Core domain.
 * They are a published contract, not an internal DTO layer: once an event has a
 * consumer, its shape is owed to that consumer indefinitely. Nothing here depends
 * on Spring, Kafka, JPA, or a JSON library, because the producer and every
 * consumer compile against this same module and none of them should drag another's
 * framework onto the others' classpath.
 *
 * <h2>Versioning</h2>
 * The version lives in the PACKAGE, not in type names. {@code ExpenseCreated},
 * never {@code ExpenseCreatedV1}.
 *
 * <ul>
 *   <li><b>Within {@code v1}, changes are additive only.</b> A new field is
 *       appended and nullable. Never remove a field, never rename one, never
 *       change its type, and never repurpose one to mean something new — a
 *       consumer reading the old meaning has no way to notice.</li>
 *   <li><b>A breaking change means a whole new {@code v2} package</b>, not a
 *       half-versioned catalog where some types are v2 and others v1. The two
 *       coexist; the producer dual-writes for the migration window; consumers move
 *       independently and {@code v1} is deleted when the last one has.</li>
 * </ul>
 *
 * There is deliberately NO {@code schemaVersion} field on the envelope. It would
 * be a second source of truth about the same fact, and nothing would resolve the
 * disagreement when a {@code v2} record carried {@code schemaVersion = 1}. What
 * the wire actually needs is a discriminator, so every event exposes
 * {@link com.vyay.events.v1.DomainEvent#eventType()} — {@code "expense.created.v1"}
 * — which is derived from the type itself and therefore cannot drift from it. Put
 * that string in the message header or the JSON discriminator; it carries the
 * version to places a package name cannot go.
 *
 * <h2>Two asymmetries worth knowing</h2>
 * Appending a field is <em>source-breaking for the producer</em> (a record's
 * canonical constructor gains a parameter) and <em>wire-compatible for consumers</em>
 * that use a tolerant reader. That is the right way round: the producer is one
 * codebase we control, consumers are many and are not ours to recompile.
 *
 * Adding a whole new event type is source-breaking for any consumer that switches
 * exhaustively over the sealed hierarchy. A consumer that wants additive event
 * types to stay non-breaking should include a {@code default} branch; one that
 * wants to be told about every new type should omit it. Both are legitimate, and
 * sealing is what makes the choice available.
 *
 * <h2>What payloads carry</h2>
 * Complete DOMAIN data and no display data — IDs, never names or avatars. A client
 * resolves identity from its own member store. The sole exception is
 * {@link com.vyay.events.v1.UserCreated} / {@link com.vyay.events.v1.UserUpdated},
 * which exist precisely to feed a server-side user reference projection for email
 * digests and notification copy; they are the identity feed, so they carry
 * identity.
 *
 * <h2>Why every type is in one package</h2>
 * {@code sealed} with {@code permits} requires all permitted subtypes in the same
 * package when the jar is on the classpath rather than the module path. Splitting
 * into {@code expense}, {@code settlement}, {@code group} sub-packages would cost
 * either a {@code module-info.java} or the sealing, and exhaustive matching on
 * event type is worth more than the folders.
 */
package com.vyay.events.v1;
