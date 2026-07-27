package com.vyay.core.dto.base;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class VersionedRequest {

    /**
     * Version of the resource the client last retrieved.
     * Used for optimistic concurrency control to prevent stale updates.
     *
     * If the resource has been modified since this version was read,
     * the update is rejected with 409 Conflict (ERR_STALE_VERSION).
     */
    @NotNull(message = "ifMatchVersion is required for edit operations")
    private Long ifMatchVersion;
}