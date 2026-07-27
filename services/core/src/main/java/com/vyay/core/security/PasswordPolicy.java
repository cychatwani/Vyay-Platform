package com.vyay.core.security;

import java.util.List;

public interface PasswordPolicy {

    /**
     * Returns an empty list if the password passes all rules.
     * Returns a list of human-readable violation messages otherwise.
     */
    List<String> validate(String password);

    /**
     * Check if password appears in known breaches.
     * Default returns false (not breached) — override with real check later.
     */
    default boolean isBreached(String password) {
        return false;
    }
}