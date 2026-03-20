// user-profile-service/src/main/java/com/shoppingfeed/profile/repository/UserProfileRepository.java
package com.shoppingfeed.profile.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shoppingfeed.profile.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class UserProfileRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis key prefix — all profile keys look like "profile:user-001"
    private static final String KEY_PREFIX = "profile:";

    // How long to keep a profile in Redis (30 days)
    // If a user is inactive for 30 days, their profile expires
    private static final long TTL_DAYS = 30;

    public UserProfileRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // ObjectMapper converts between Java objects and JSON strings
        // JavaTimeModule handles Instant, LocalDate etc.
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());
    }

    /**
     * Save a profile to Redis.
     * We convert the UserProfile object to a JSON string first,
     * then store that string in Redis.
     */
    public void save(UserProfile profile) {
        try {
            String key = KEY_PREFIX + profile.getUserId();
            String json = objectMapper.writeValueAsString(profile);
            redisTemplate.opsForValue().set(key, json, TTL_DAYS, TimeUnit.DAYS);
            log.debug("Saved profile for user: {}", profile.getUserId());
        } catch (Exception e) {
            log.error("Failed to save profile for user {}: {}",
                    profile.getUserId(), e.getMessage());
        }
    }

    /**
     * Find a profile by userId.
     * Returns Optional.empty() if the user has no profile yet
     * (new user — cold start case)
     */
    public Optional<UserProfile> findByUserId(String userId) {
        try {
            String key = KEY_PREFIX + userId;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, UserProfile.class));
        } catch (Exception e) {
            log.error("Failed to read profile for user {}: {}",
                    userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Delete a profile (used when user requests data deletion)
     */
    public void deleteByUserId(String userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}