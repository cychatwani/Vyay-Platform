package com.vyay.core.security;

import com.vyay.core.enums.TokenType;

import java.util.List;
import java.util.UUID;

/**
 * Typed, exhaustive taxonomy of every JWT this app issues. One record per token
 * type — each type's claim shape is visible here, and adding a new TokenType
 * forces the parse switch in JwtService to handle it.
 */
public sealed interface TokenClaims {

    String subject();
    TokenType type();

    record Access(String subject) implements TokenClaims {
        @Override public TokenType type() { return TokenType.ACCESS; }
    }

    record Refresh(String subject) implements TokenClaims {
        @Override public TokenType type() { return TokenType.REFRESH; }
    }

    record EmailVerification(String subject) implements TokenClaims {
        @Override public TokenType type() { return TokenType.EMAIL_VERIFICATION; }
    }

    record GroupInvite(String subject, List<String> invitedUsers) implements TokenClaims {
        public GroupInvite {
            invitedUsers = (invitedUsers == null) ? List.of() : List.copyOf(invitedUsers);
        }

        @Override public TokenType type() { return TokenType.GROUP_INVITE; }

        public String code() { return subject; }

        public boolean isOpen() { return invitedUsers.isEmpty(); }

        /** Empty/absent list = anyone; otherwise the joiner's userId must be present. */
        public boolean allows(UUID userId) {
            return isOpen() || invitedUsers.contains(userId.toString());
        }
    }
}
