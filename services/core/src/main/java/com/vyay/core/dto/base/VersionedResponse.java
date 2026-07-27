package com.vyay.core.dto.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@SuperBuilder
public abstract class VersionedResponse {

    /**
     * Current resource version used for optimistic concurrency control.
     */
    private Long version;
}