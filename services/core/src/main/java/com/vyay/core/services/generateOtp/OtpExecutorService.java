package com.vyay.core.services.generateOtp;

import com.vyay.core.config.OtpProperties;
import com.vyay.core.exception.business.InvalidOtpException;
import com.vyay.core.exception.business.OtpMaxAttemptsExceededException;
import com.vyay.core.exception.business.OtpResendCooldownException;
import com.vyay.core.exception.business.OtpVerificationNotFoundException;
import com.vyay.core.services.notification.NotificationCommand;
import com.vyay.core.services.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.vyay.core.services.generateOtp.OtpRedisSchema.*;

@Service
public class OtpExecutorService {

    private static final Logger log = LoggerFactory.getLogger(OtpExecutorService.class);

    private static final String VAR_OTP = "otp";
    private static final String VAR_EXPIRY_MINUTES = "expiryMinutes";

    private final StringRedisTemplate redisTemplate;
    private final OtpGenerator otpGenerator;
    private final OtpProperties otpProperties;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public OtpExecutorService(StringRedisTemplate redisTemplate,
                              OtpGenerator otpGenerator,
                              OtpProperties otpProperties,
                              NotificationService notificationService,
                              PasswordEncoder passwordEncoder) {
        this.redisTemplate = redisTemplate;
        this.otpGenerator = otpGenerator;
        this.otpProperties = otpProperties;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    public OtpChallenge issue(UUID userId, Collection<NotificationCommand> commands) {
        supersedeExistingChallenge(userId);

        String verificationId = UUID.randomUUID().toString();
        String otp = otpGenerator.generate(otpProperties.length());
        String otpHash = passwordEncoder.encode(otp);
        Instant now = Instant.now();

        storeChallenge(verificationId, userId, otpHash);
        setCooldown(userId);


        // TODO: When NotificationService is backed by an Outbox (Postgres), Redis and the Outbox
        //  cannot share a transaction. If Outbox persistence fails after the Redis challenge is
        //  already stored, an orphaned OTP exists that can never be delivered. Add a compensation
        //  strategy: catch the Outbox failure and delete the Redis challenge + cooldown keys
        //  (verificationKey, userKey, cooldownKey) before re-throwing.

        List<NotificationCommand> enriched = enrichCommands(commands, otp);
        notificationService.send(enriched);

        log.info("OTP challenge issued for userId={}, verificationId={}", userId, verificationId);

        return OtpChallenge.builder()
                .verificationId(verificationId)
                .expiresAt(now.plus(otpProperties.ttl()))
                .resendAvailableAt(now.plus(otpProperties.resendCooldown()))
                .build();
    }

    public UUID verify(String verificationId, String submittedOtp) {
        String vKey = verificationKey(verificationId);

        // Atomic increment before read — concurrent requests cannot skip the counter
        Long newAttempts = redisTemplate.opsForHash().increment(vKey, FIELD_ATTEMPTS, 1);

        // HINCRBY on a missing key creates an orphan with just {attempts: 1}.
        // Detect by checking for our data fields.
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(vKey);
        String storedHash = (String) entries.get(FIELD_OTP_HASH);
        if (storedHash == null) {
            redisTemplate.delete(vKey);
            throw new OtpVerificationNotFoundException();
        }

        UUID userId = UUID.fromString((String) entries.get(FIELD_USER_ID));

        if (newAttempts > otpProperties.maxAttempts()) {
            cleanupChallenge(vKey, userId);
            throw new OtpMaxAttemptsExceededException();
        }

        if (!passwordEncoder.matches(submittedOtp, storedHash)) {
            int remaining = otpProperties.maxAttempts() - newAttempts.intValue();
            if (remaining <= 0) {
                cleanupChallenge(vKey, userId);
                throw new OtpMaxAttemptsExceededException();
            }
            throw new InvalidOtpException(remaining);
        }

        cleanupChallenge(vKey, userId);
        log.info("OTP verified for userId={}, verificationId={}", userId, verificationId);
        return userId;
    }

    public OtpChallenge resend(UUID userId, Collection<NotificationCommand> commands) {
        String cdKey = cooldownKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cdKey))) {
            Long ttlSeconds = redisTemplate.getExpire(cdKey);
            throw new OtpResendCooldownException(ttlSeconds != null ? ttlSeconds : 0);
        }

        // issue() supersedes the existing challenge automatically
        return issue(userId, commands);
    }

    private List<NotificationCommand> enrichCommands(Collection<NotificationCommand> commands, String otp) {
        Map<String, String> otpVariables = Map.of(
                VAR_OTP, otp,
                VAR_EXPIRY_MINUTES, String.valueOf(otpProperties.ttl().toMinutes())
        );

        return commands.stream()
                .map(cmd -> {
                    Map<String, String> merged = new HashMap<>(cmd.getVariables());
                    merged.putAll(otpVariables);
                    return NotificationCommand.builder()
                            .type(cmd.getType())
                            .channel(cmd.getChannel())
                            .recipient(cmd.getRecipient())
                            .variables(Map.copyOf(merged))
                            .build();
                })
                .toList();
    }

    private void supersedeExistingChallenge(UUID userId) {
        String existingVerificationId = redisTemplate.opsForValue().get(userKey(userId));
        if (existingVerificationId != null) {
            redisTemplate.delete(verificationKey(existingVerificationId));
        }
    }

    private void storeChallenge(String verificationId, UUID userId, String otpHash) {
        String vKey = verificationKey(verificationId);
        redisTemplate.opsForHash().putAll(vKey, Map.of(
                FIELD_USER_ID, userId.toString(),
                FIELD_OTP_HASH, otpHash,
                FIELD_ATTEMPTS, "0"
        ));
        redisTemplate.expire(vKey, otpProperties.ttl());

        redisTemplate.opsForValue().set(userKey(userId), verificationId, otpProperties.ttl());
    }

    private void setCooldown(UUID userId) {
        redisTemplate.opsForValue().set(cooldownKey(userId), "1", otpProperties.resendCooldown());
    }

    private void cleanupChallenge(String vKey, UUID userId) {
        redisTemplate.delete(List.of(vKey, userKey(userId), cooldownKey(userId)));
    }
}
