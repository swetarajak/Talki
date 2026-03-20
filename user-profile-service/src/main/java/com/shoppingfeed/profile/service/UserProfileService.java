// user-profile-service/src/main/java/com/shoppingfeed/profile/service/UserProfileService.java
package com.shoppingfeed.profile.service;

import com.shoppingfeed.common.event.UserEvent;
import com.shoppingfeed.profile.model.UserProfile;
import com.shoppingfeed.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository profileRepository;

    /**
     * Decay factor — makes recent events matter more than old ones.
     *
     * When we update a score, we multiply the existing score by this factor.
     * 0.95 means old scores slowly shrink over time.
     *
     * Example:
     *   User viewed dresses 10 times last month → score = 0.8
     *   User hasn't viewed dresses this week
     *   After decay: 0.8 × 0.95 = 0.76 (score slowly fades)
     *   If user views dresses again: 0.76 + boost = higher score
     */
    private static final double DECAY_FACTOR = 0.95;

    /**
     * Score boosts per event type.
     * A purchase signals much stronger preference than a scroll.
     */
    private static final double BOOST_VIEW          = 0.05;
    private static final double BOOST_CLICK         = 0.10;
    private static final double BOOST_ADD_TO_CART   = 0.20;
    private static final double BOOST_PURCHASE      = 0.40;
    private static final double BOOST_SCROLL        = 0.01;

    /**
     * Main method — called every time a new event arrives from Kafka.
     * Finds or creates the user's profile, updates it, saves it back.
     */
    public void processEvent(UserEvent event) {

        // 1. Load existing profile, or create a fresh one for new users
        UserProfile profile = profileRepository
                .findByUserId(event.getUserId())
                .orElseGet(() -> createNewProfile(event.getUserId()));

        // 2. Update the profile based on what the user did
        updateProfile(profile, event);

        // 3. Save back to Redis
        profileRepository.save(profile);

        log.debug("Updated profile for user {} after {} event",
                event.getUserId(), event.getEventType());
    }

    /**
     * Creates a brand new empty profile for a first-time user
     */
    private UserProfile createNewProfile(String userId) {
        return UserProfile.builder()
                .userId(userId)
                .createdAt(Instant.now())
                .lastUpdated(Instant.now())
                .totalEvents(0)
                .minPrice(0)
                .maxPrice(0)
                .avgPrice(0)
                .build();
    }

    /**
     * Updates the profile scores based on the incoming event.
     *
     * The formula is:
     *   newScore = (oldScore × DECAY_FACTOR) + boost
     *
     * This is called an Exponential Moving Average (EMA).
     * It naturally gives more weight to recent behaviour.
     */
    private void updateProfile(UserProfile profile, UserEvent event) {

        double boost = getBoostForEventType(event.getEventType());

        // Update category score if present
        if (event.getProductId() != null) {
            updateScore(profile.getCategoryScores(),
                    getCategoryFromProductId(event.getProductId()), boost);
        }

        // Extra boost for dwell time — if user spent > 30 seconds, they're interested
        if (event.getEventType() == UserEvent.EventType.VIEW
                && event.getDwellTimeSeconds() > 30) {
            updateScore(profile.getCategoryScores(),
                    getCategoryFromProductId(event.getProductId()),
                    BOOST_VIEW * 0.5); // extra 50% boost for long views
        }

        // Increment total events count
        profile.setTotalEvents(profile.getTotalEvents() + 1);
        profile.setLastUpdated(Instant.now());
    }

    /**
     * Updates a single score in a map using the decay + boost formula.
     * Score is capped between 0.0 and 1.0
     */
    private void updateScore(java.util.Map<String, Double> scores,
                             String key, double boost) {
        double current = scores.getOrDefault(key, 0.0);
        // Apply decay to existing score, then add the new boost
        double updated = Math.min(1.0, (current * DECAY_FACTOR) + boost);
        scores.put(key, updated);
    }

    /**
     * Returns the boost amount based on event type.
     * Purchase = strongest signal. Scroll = weakest.
     */
    private double getBoostForEventType(UserEvent.EventType type) {
        return switch (type) {
            case PURCHASE      -> BOOST_PURCHASE;
            case ADD_TO_CART   -> BOOST_ADD_TO_CART;
            case CLICK         -> BOOST_CLICK;
            case VIEW          -> BOOST_VIEW;
            case SCROLL        -> BOOST_SCROLL;
            default            -> BOOST_VIEW;
        };
    }

    /**
     * In a real system, you'd look up the product in the database
     * to get its category. For now we extract it from the productId
     * convention: "dresses-001", "kurtas-023" etc.
     *
     * We'll replace this with a real DB lookup in a later step.
     */
    private String getCategoryFromProductId(String productId) {
        if (productId == null) return "unknown";
        String[] parts = productId.split("-");
        return parts.length > 0 ? parts[0] : "unknown";
    }
}