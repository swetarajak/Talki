// trending-service/src/main/java/com/shoppingfeed/trending/controller/TrendingController.java
package com.shoppingfeed.trending.controller;

import com.shoppingfeed.trending.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trending")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    /**
     * GET /api/v1/trending?limit=10
     * Returns top trending product IDs right now.
     * The AI engine calls this when building recommendations.
     */
    @GetMapping
    public ResponseEntity<List<String>> getTopTrending(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(trendingService.getTopTrending(limit));
    }

    /**
     * GET /api/v1/trending/scores?limit=10
     * Returns top trending products WITH their scores.
     * Useful for debugging and admin dashboards.
     */
    @GetMapping("/scores")
    public ResponseEntity<List<String>> getTopTrendingWithScores(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
                trendingService.getTopTrendingWithScores(limit));
    }

    /**
     * GET /api/v1/trending/{productId}/score
     * Returns the trending score for one specific product.
     */
    @GetMapping("/{productId}/score")
    public ResponseEntity<Map<String, Object>> getProductScore(
            @PathVariable String productId) {
        double score = trendingService.getTrendingScore(productId);
        return ResponseEntity.ok(Map.of(
                "productId", productId,
                "trendingScore", score
        ));
    }
}