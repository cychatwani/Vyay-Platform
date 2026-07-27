package com.vyay.core.common.security;

import com.github.f4b6a3.uuid.UuidCreator;
import com.vyay.core.entity.User;
import com.vyay.core.entity.balance.BalanceLedgerEntry;
import com.vyay.core.entity.group.Group;
import com.vyay.core.entity.reference.Currency;
import com.vyay.core.enums.LedgerSourceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public HmacSigner(@Value("${app.ledger.hmac-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public BalanceLedgerEntry signedEntry(Group group, User user, Currency currency,
                                          long deltaMinor, LedgerSourceType sourceType, UUID sourceId) {
        UUID id = UuidCreator.getTimeOrderedEpoch();
        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        String hmac = hmacHex(canonical(
                id, group.getId(), user.getId(), currency.getCode(),
                deltaMinor, sourceType, sourceId, createdAt));

        return BalanceLedgerEntry.builder()
                .id(id)
                .group(group)
                .user(user)
                .currency(currency)
                .deltaMinor(deltaMinor)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .createdAt(createdAt)
                .hmac(hmac)
                .build();
    }

    public boolean verify(BalanceLedgerEntry e) {
        String expected = hmacHex(canonical(
                e.getId(), e.getGroup().getId(), e.getUser().getId(), e.getCurrency().getCode(),
                e.getDeltaMinor(), e.getSourceType(), e.getSourceId(), e.getCreatedAt()));
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                e.getHmac().getBytes(StandardCharsets.UTF_8));
    }

    private String canonical(UUID id, UUID groupId, UUID userId, String currencyCode,
                             long deltaMinor, LedgerSourceType sourceType, UUID sourceId,
                             Instant createdAt) {
        return String.join("|",
                id.toString(),
                groupId.toString(),
                userId.toString(),
                currencyCode,
                String.valueOf(deltaMinor),
                sourceType.name(),
                sourceId.toString(),
                String.valueOf(createdAt.toEpochMilli()));
    }

    private String hmacHex(String canonical) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to compute ledger HMAC", ex);
        }
    }
}
