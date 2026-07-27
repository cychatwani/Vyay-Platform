package com.vyay.core.enums;

/**
 * Governs who — if anyone — may record a settlement between two OTHER members
 * (i.e. when the authenticated user is neither the payer nor the payee).
 * Does not affect settlements where the actor is one of the two parties.
 */
public enum ThirdPartySettlementPolicy {

    /** No third-party recording. Only a settlement's own parties may create it. */
    DISABLED,

    /** Only group admins may record settlements on behalf of other members. */
    ADMIN_ONLY,

    /** Any active member may record settlements between other members. */
    ALL_MEMBERS
}