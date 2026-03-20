// ai-recommendation-engine/src/main/java/com/shoppingfeed/ai/consumer/ProfileUpdateConsumer.java
package com.shoppingfeed.ai.consumer;

import com.shoppingfeed.ai.service.FeedPublisherService;
import com.shoppingfeed.ai.service.RecommendationService;
import com.shoppingfeed.common.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileUpdateConsumer {

    private final RecommendationService recommendationService;
    private final FeedPublisherService feedPublisherService;

    /**
     * Listens to user-events topic.
     * Every 10 events per user, regenerate their feed.
     *
     * In production you'd use a smarter trigger — but this
     * works well for our learning project.
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "ai-recommendation-engine"
    )
    public void onUserEvent(@Payload UserEvent event) {
        try {
            // Only regenerate feed for strong signals
            if (event.getEventType() == UserEvent.EventType.PURCHASE ||
                    event.getEventType() == UserEvent.EventType.ADD_TO_CART ||
                    event.getEventType() == UserEvent.EventType.CLICK) {

                log.info("Strong signal detected for user {} — regenerating feed",
                        event.getUserId());

                List<Map<String, Object>> feed =
                        recommendationService.getRecommendations(event.getUserId());

                feedPublisherService.publishFeed(event.getUserId(), feed);
            }
        } catch (Exception e) {
            log.error("Failed to process event for recommendations: {}",
                    e.getMessage());
        }
    }
}