// user-profile-service/src/main/java/com/shoppingfeed/profile/model/UserProfile.java
package com.shoppingfeed.profile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Serializable is required because Redis needs to convert
 * this object to bytes to store it.
 *
 * Think of this as the "taste card" for each user.
 * Every field is a score between 0.0 and 1.0
 * Higher score = stronger preference
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile implements Serializable {

    private String userId;

    /**
     * Category preferences
     * e.g. { "dresses": 0.8, "kurtas": 0.6, "western": 0.1 }
     */
    @Builder.Default
    private Map<String, Double> categoryScores = new HashMap<>();

    /**
     * Brand preferences
     * e.g. { "Fabindia": 0.7, "Zara": 0.3 }
     */
    @Builder.Default
    private Map<String, Double> brandScores = new HashMap<>();

    /**
     * Style tag preferences
     * e.g. { "floral": 0.9, "casual": 0.6, "formal": 0.2 }
     */
    @Builder.Default
    private Map<String, Double> tagScores = new HashMap<>();

    // Price range the user tends to shop in
    private double minPrice;
    private double maxPrice;
    private double avgPrice;

    // How many events we've seen from this user
    private int totalEvents;

    private Instant lastUpdated;
    private Instant createdAt;
}