// ai-recommendation-engine/src/main/java/com/shoppingfeed/ai/controller/RecommendationController.java
package com.shoppingfeed.ai.controller;

import com.shoppingfeed.ai.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * GET /api/v1/recommendations/{userId}
     * Returns AI-ranked personalized feed for a user.
     *
     * The Feed Delivery Service calls this to get
     * the feed to push to the user.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(
            @PathVariable String userId) {
        List<Map<String, Object>> feed =
                recommendationService.getRecommendations(userId);
        return ResponseEntity.ok(feed);
    }
}