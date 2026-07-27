package com.vyay.core.security;

import com.vyay.core.enums.TokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;


@Component
public class JwtService {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final long emailVerificationExpiration;

    public JwtService(
            @Value("${spring.jwt.secret}") String secret,
            @Value("${spring.jwt.access-token-expiration}") long accessExpiration,
            @Value("${spring.jwt.refresh-token-expiration}") long refreshExpiration,
            @Value("${spring.jwt.email-verification-expiration}") long emailVerificationExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.emailVerificationExpiration = emailVerificationExpiration;
    }

    public String generateAccessToken(UUID userId) {
        return buildToken(userId.toString(), TokenType.ACCESS.getValue(), accessExpiration);
    }

    public String generateRefreshToken(UUID userId) {
        return buildToken(userId.toString(), TokenType.REFRESH.getValue(), refreshExpiration);
    }

    public String generateEmailVerificationToken(UUID userId) {
        return buildToken(userId.toString(), TokenType.EMAIL_VERIFICATION.getValue(), emailVerificationExpiration);
    }

    public String generateGroupInviteToken(String code, List<UUID> invitedUserIds, long ttl) {
        Map<String, Object> extra = (invitedUserIds == null || invitedUserIds.isEmpty())
                ? null
                : Map.of("invitedUsers", invitedUserIds.stream().map(UUID::toString).toList());
        return buildToken(code, TokenType.GROUP_INVITE.getValue(), extra, ttl);
    }

    private String buildToken(String subject, String type, Map<String, Object> extraClaims, long ttl) {
        var builder = Jwts.builder()
                .subject(subject)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttl));
        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }
        return builder.signWith(key).compact();
    }

    private String buildToken(String subject, String type, long ttl) {
        return buildToken(subject, type, null, ttl);
    }

    public TokenClaims parse(String token) {
        Claims c = verifyAndParse(token);
        try {
            TokenType type = TokenType.fromValue(c.get("type", String.class));
            return switch (type) {
                case ACCESS -> new TokenClaims.Access(c.getSubject());
                case REFRESH -> new TokenClaims.Refresh(c.getSubject());
                case EMAIL_VERIFICATION -> new TokenClaims.EmailVerification(c.getSubject());
                case GROUP_INVITE -> {
                    @SuppressWarnings("unchecked")
                    List<String> invited = (List<String>) c.get("invitedUsers", List.class);
                    yield new TokenClaims.GroupInvite(c.getSubject(), invited);
                }
            };
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException("Malformed token claims", e);
        }
    }

    public TokenClaims.GroupInvite parseGroupInvite(String token) {
        if (parse(token) instanceof TokenClaims.GroupInvite invite) {
            return invite;
        }
        throw new BadCredentialsException("Invalid token type");
    }

    private Claims verifyAndParse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new BadCredentialsException("Token has expired", e);
        } catch (SecurityException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid JWT token", e);
        }
    }

    public UUID getUserIdFromToken(String token, String allowedTokenType) {
        TokenClaims claims = parse(token);
        if (!claims.type().getValue().equals(allowedTokenType)) {
            throw new BadCredentialsException("Invalid token type");
        }
        try {
            return UUID.fromString(claims.subject());
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid user id in token", e);
        }
    }

    public String getSubject(String token) {
        return parse(token).subject();
    }

    public String getType(String token) {
        return parse(token).type().getValue();
    }
}
