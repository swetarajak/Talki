package com.shoppingfeed.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedPublisherService {

    // Changed to String — publish JSON as plain string
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FEED_TOPIC = "feed-updates";

    public void publishFeed(String userId,
                            List<Map<String, Object>> feed) {
        try {
            Map<String, Object> feedUpdate = Map.of(
                    "userId", userId,
                    "feed", feed,
                    "generatedAt", System.currentTimeMillis()
            );

            // Convert to JSON string before publishing
            String json = objectMapper.writeValueAsString(feedUpdate);
            kafkaTemplate.send(FEED_TOPIC, userId, json);

            log.info("Published feed update for user {} " +
                    "with {} items", userId, feed.size());

        } catch (Exception e) {
            log.error("Failed to publish feed for user {}: {}",
                    userId, e.getMessage());
        }
    }
}