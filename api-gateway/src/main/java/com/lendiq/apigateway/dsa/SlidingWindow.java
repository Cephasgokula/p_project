package com.lendiq.apigateway.dsa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Sliding Window velocity fraud detection backed by Redis sorted sets.
 * If the same IP/device submits more than N applications within a time window,
 * the request is flagged before it enters Kafka — saving ML inference cost on obvious fraud.
 *
 * Redis key: velocity:{ip_hash}:{device_fp}
 * Value: sorted set of timestamps (score = epoch millis, member = unique ID)
 * O(1) amortised operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlidingWindow {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "velocity:";

    /**
     * Check if the given IP + device fingerprint exceeds the velocity threshold.
     *
     * @param ipHash             Hashed IP address
     * @param deviceFingerprint  Device fingerprint hash
     * @param threshold          Max allowed requests in the window
     * @param windowSeconds      Window duration in seconds
     * @return true if velocity threshold exceeded (fraud detected)
     */
    public boolean isVelocityFraud(String ipHash, String deviceFingerprint,
                                    int threshold, int windowSeconds) {
        String key = KEY_PREFIX + ipHash + ":" + deviceFingerprint;
        Instant now = Instant.now();
        long windowStart = now.minusSeconds(windowSeconds).toEpochMilli();
        long nowMs = now.toEpochMilli();

        ZSetOperations<String, String> zops = redisTemplate.opsForZSet();

        // Evict expired entries outside the window
        zops.removeRangeByScore(key, 0, windowStart);

        // Count current entries in the window
        Long count = zops.zCard(key);

        // Add current request timestamp with a unique member
        zops.add(key, UUID.randomUUID().toString(), nowMs);

        // Set TTL to auto-expire the key after the window passes
        redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));

        boolean exceeded = count != null && count >= threshold;
        if (exceeded) {
            log.warn("Velocity fraud detected: key={}, count={}, threshold={}",
                key, count, threshold);
        }

        return exceeded;
    }
}
