// trending-service/src/main/java/com/shoppingfeed/trending/service/TrendingService.java
package com.shoppingfeed.trending.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrendingService {

    private final RedisTemplate<String, String> redisTemplate;

    // Redis sorted set key for trending products
    private static final String TRENDING_KEY = "trending:products";

    /**
     * Updates the trending score for a product.
     *
     * Redis sorted sets work like this:
     *   Key   = "trending:products"
     *   Value = productId (e.g. "dresses-123")
     *   Score = trending score (e.g. 42.5)
     *
     * Redis automatically keeps them sorted by score.
     * So getting top 10 is just one Redis command.
     */
    public void updateTrendingScore(String productId, double score) {
        redisTemplate.opsForZSet().add(TRENDING_KEY, productId, score);
        log.debug("Updated trending score for {}: {}", productId, score);
    }

    /**
     * Returns top N trending products right now.
     * Redis returns them sorted highest score first.
     *
     * This is called by the AI engine when building
     * the personalized feed — it boosts products that
     * are both relevant AND trending.
     */
    public List<String> getTopTrending(int limit) {
        // ZREVRANGE — get members in REVERSE order (highest score first)
        Set<String> topProducts = redisTemplate.opsForZSet()
                .reverseRange(TRENDING_KEY, 0, limit - 1);

        if (topProducts == null) return new ArrayList<>();
        return new ArrayList<>(topProducts);
    }

    /**
     * Get trending score for a specific product.
     * Returns 0.0 if product has no trending score.
     */
    public double getTrendingScore(String productId) {
        Double score = redisTemplate.opsForZSet()
                .score(TRENDING_KEY, productId);
        return score != null ? score : 0.0;
    }

    /**
     * Returns top N products WITH their scores.
     * Used by the controller to expose the full leaderboard.
     */
    public List<String> getTopTrendingWithScores(int limit) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>>
                results = redisTemplate.opsForZSet()
                .reverseRangeWithScores(TRENDING_KEY, 0, limit - 1);

        List<String> output = new ArrayList<>();
        if (results != null) {
            results.forEach(tuple ->
                    output.add(tuple.getValue() + " → " +
                            String.format("%.1f", tuple.getScore()))
            );
        }
        return output;
    }
}