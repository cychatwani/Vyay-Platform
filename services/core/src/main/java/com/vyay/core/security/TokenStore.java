package com.vyay.core.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenStore {

    private final StringRedisTemplate redisTemplate;

    private String prepareTokenKey(UUID userId) {
        return "RefreshToken_" + userId;
    }

    public void storeRefreshToken(UUID userId, String refreshToken, long ttlMillis) {
        redisTemplate.opsForValue()
                .set(prepareTokenKey(userId), refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
    }

    public Optional<String> getRefreshToken(UUID userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(prepareTokenKey(userId)));
    }

    public void deleteRefreshToken(UUID userId) {
        redisTemplate.delete(prepareTokenKey(userId));
    }
}
