package com.vyay.core.enums;

/**
 * The mechanism by which a verification challenge is delivered to, and completed by, a subject.
 *
 * <p>A channel answers <em>how</em> a subject proves something, and is deliberately decoupled from
 * {@code VerificationPurpose}, which answers <em>what</em> is being proved. The same channel may
 * serve many purposes (an email OTP can verify a new registration or authorize a sensitive action),
 * and a single purpose may permit several channels, with the allowed set governed by per-purpose
 * policy rather than by this enum.
 *
 * <p>This enum is never persisted. It appears only in configuration policy and in ephemeral
 * challenge records held in Redis, so it requires no database type and no JDBC type mapping.
 *
 * <p>Constants are added here only once a handler exists to service them. An unbacked constant is a
 * latent runtime failure: it can be referenced from configuration and will fault at challenge time
 * rather than at startup. Channels on the roadmap — SMS OTP, TOTP, and passkey among them — are
 * therefore intentionally absent until their implementations land.
 */
public enum PasswordRegistrationVerificationChannel {

    /**
     * A short numeric code, generated server-side and delivered to the subject's email address.
     *
     * <p>The code is stored only as a hash on the challenge record and is submitted back by the
     * client to complete the challenge. Suited to clients that cannot reliably handle a deep link,
     * most notably the mobile application.
     */
    EMAIL_OTP,

    /**
     * A single-use link, delivered to the subject's email address, which the subject follows to
     * complete the challenge.
     *
     * <p>Completion happens in the browser that opens the link rather than in the client that
     * requested the challenge, which makes this the natural choice for the web application and an
     * awkward one for mobile.
     */
    EMAIL_LINK
}