// user-profile-service/src/main/java/com/shoppingfeed/profile/consumer/UserEventConsumer.java
package com.shoppingfeed.profile.consumer;

import com.shoppingfeed.common.event.UserEvent;
import com.shoppingfeed.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserProfileService profileService;

    /**
     * @KafkaListener is the magic annotation.
     * Spring automatically calls this method every time a new
     * message arrives on the "user-events" topic.
     *
     * topics    = which Kafka topic to listen to
     * groupId   = consumer group name (explained below)
     */
    @KafkaListener(
            topics = "user-events",
            groupId = "user-profile-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload UserEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed event [{}] from user {} | partition={} offset={}",
                event.getEventType(),
                event.getUserId(),
                partition,
                offset);

        try {
            profileService.processEvent(event);
        } catch (Exception e) {
            // Log the error but don't crash — next message should still be processed
            log.error("Failed to process event for user {}: {}",
                    event.getUserId(), e.getMessage());
        }
    }
}