package com.vyay.core.enums;

public enum TokenType {
    ACCESS("access"),
    REFRESH("refresh"),
    EMAIL_VERIFICATION("email_verification"),
        GROUP_INVITE("group_invite");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    /** String form for storage in JWT claims, DB, etc. */
    public String getValue() {
        return value;
    }

    /** Convert a string back to enum safely. */
    public static TokenType fromValue(String value) {
        for (TokenType type : TokenType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown token type: " + value);
    }
}
