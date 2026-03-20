package com.shoppingfeed.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shoppingfeed.ai.model.Product;
import com.shoppingfeed.ai.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final VectorStore vectorStore;
    private final ProductRepository productRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${spring.ai.openai.api-key}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String PROFILE_KEY_PREFIX = "profile:";
    private static final String TRENDING_KEY = "trending:products";
    private static final String GROQ_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final int MAX_CANDIDATES = 20;
    private static final int FEED_SIZE = 10;

    public List<Map<String, Object>> getRecommendations(String userId) {
        log.info("Generating recommendations for user: {}", userId);

        Map<String, Object> userProfile = loadUserProfile(userId);
        List<String> trendingProducts = getTrendingProducts();
        String searchQuery = buildSearchQuery(userProfile);

        log.debug("Search query for user {}: {}", userId, searchQuery);

        List<Document> similarProducts = searchVectorStore(searchQuery);
        List<Product> candidates = getProductDetails(similarProducts);

        List<Map<String, Object>> rankedFeed =
                rankWithAI(userId, userProfile, candidates, trendingProducts);

        log.info("Generated {} recommendations for user {}",
                rankedFeed.size(), userId);
        return rankedFeed;
    }

    private Map<String, Object> loadUserProfile(String userId) {
        try {
            String json = redisTemplate.opsForValue()
                    .get(PROFILE_KEY_PREFIX + userId);
            if (json == null) return new HashMap<>();
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to load profile for {}: {}",
                    userId, e.getMessage());
            return new HashMap<>();
        }
    }

    private List<String> getTrendingProducts() {
        Set<String> trending = redisTemplate.opsForZSet()
                .reverseRange(TRENDING_KEY, 0, 9);
        return trending != null
                ? new ArrayList<>(trending)
                : new ArrayList<>();
    }

    private String buildSearchQuery(Map<String, Object> profile) {
        if (profile.isEmpty()) {
            return "popular trending products women fashion";
        }

        StringBuilder query = new StringBuilder();

        Map<String, Double> categories =
                (Map<String, Double>) profile.getOrDefault(
                        "categoryScores", Map.of());
        categories.entrySet().stream()
                .filter(e -> e.getValue() > 0.1)
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed())
                .limit(3)
                .forEach(e -> query.append(e.getKey()).append(" "));

        Map<String, Double> tags =
                (Map<String, Double>) profile.getOrDefault(
                        "tagScores", Map.of());
        tags.entrySet().stream()
                .filter(e -> e.getValue() > 0.1)
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .reversed())
                .limit(5)
                .forEach(e -> query.append(e.getKey()).append(" "));

        String result = query.toString().trim();
        return result.isEmpty()
                ? "popular trending fashion products"
                : result;
    }

    private List<Document> searchVectorStore(String query) {
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.query(query)
                            .withTopK(MAX_CANDIDATES)
                            .withSimilarityThreshold(0.3)
            );
        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Product> getProductDetails(List<Document> documents) {
        List<String> productIds = documents.stream()
                .map(doc -> (String) doc.getMetadata().get("productId"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return productRepository.findByActiveTrue();
        }
        return productRepository.findByIdIn(productIds);
    }

    private List<Map<String, Object>> rankWithAI(
            String userId,
            Map<String, Object> profile,
            List<Product> candidates,
            List<String> trending) {

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = buildRankingPrompt(
                    userId, profile, candidates, trending);
            String aiResponse = callGroqDirectly(prompt);
            log.debug("Groq response: {}", aiResponse);
            return parseAiResponse(aiResponse, candidates);

        } catch (Exception e) {
            log.error("AI ranking failed: {}", e.getMessage());
            return candidates.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getAverageRating(),
                            a.getAverageRating()))
                    .limit(FEED_SIZE)
                    .map(this::productToMap)
                    .collect(Collectors.toList());
        }
    }

    private String callGroqDirectly(String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "llama-3.1-8b-instant");
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 1000);

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "You are a shopping assistant. " +
                        "Always respond with valid JSON only. No extra text.");

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        requestBody.put("messages", List.of(systemMsg, userMsg));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                GROQ_URL,
                HttpMethod.POST,
                request,
                Map.class
        );

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices =
                (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> firstChoice = choices.get(0);
        Map<String, String> message =
                (Map<String, String>) firstChoice.get("message");

        return message.get("content");
    }

    private String buildRankingPrompt(
            String userId,
            Map<String, Object> profile,
            List<Product> candidates,
            List<String> trending) {

        StringBuilder productList = new StringBuilder();
        for (int i = 0; i < candidates.size(); i++) {
            Product p = candidates.get(i);
            productList.append(String.format(
                    "%d. ID:%s NAME:%s CATEGORY:%s PRICE:%s RATING:%.1f\n",
                    i + 1, p.getId(), p.getName(),
                    p.getCategory(), p.getPrice(),
                    p.getAverageRating()
            ));
        }

        Map<String, Double> categories =
                (Map<String, Double>) profile.getOrDefault(
                        "categoryScores", Map.of());

        return String.format("""
            Rank these products for user %s.
            User likes: %s
            Trending now: %s
            
            Products:
            %s
            
            Return JSON only, no other text:
            {"recommendations":[{"productId":"id","rank":1,"reason":"why"}]}
            """,
                userId,
                categories.toString(),
                trending.toString(),
                productList.toString()
        );
    }

    private List<Map<String, Object>> parseAiResponse(
            String aiResponse, List<Product> candidates) {
        try {
            // Clean response — sometimes LLM adds markdown
            String cleaned = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            Map<String, Object> parsed =
                    objectMapper.readValue(cleaned, Map.class);
            List<Map<String, Object>> recommendations =
                    (List<Map<String, Object>>) parsed
                            .get("recommendations");

            Map<String, Product> productMap = candidates.stream()
                    .collect(Collectors.toMap(
                            Product::getId, p -> p));

            return recommendations.stream()
                    .map(rec -> {
                        String productId =
                                (String) rec.get("productId");
                        Product product = productMap.get(productId);
                        if (product == null) return null;
                        Map<String, Object> result =
                                productToMap(product);
                        result.put("rank", rec.get("rank"));
                        result.put("reason", rec.get("reason"));
                        return result;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}",
                    e.getMessage());
            return candidates.stream()
                    .limit(FEED_SIZE)
                    .map(this::productToMap)
                    .collect(Collectors.toList());
        }
    }

    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", product.getId());
        map.put("name", product.getName());
        map.put("category", product.getCategory());
        map.put("brand", product.getBrand());
        map.put("price", product.getPrice());
        map.put("rating", product.getAverageRating());
        map.put("tags", product.getTagList());
        map.put("imageUrl", product.getImageUrl());
        return map;
    }
}