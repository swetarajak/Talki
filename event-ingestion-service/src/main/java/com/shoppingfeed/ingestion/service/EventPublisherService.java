package com.shoppingfeed.ingestion.service;

import com.shoppingfeed.common.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherService {
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    //Topic name - matches what we'll define in application.yml
    private static final String TOPIC = "user-events";

    /*
    * takes a raw UserEvent, enriches it(ads ID + timestamp),
    * and publishes it to the kafka "user-events" topic
    * */
    public void publishEvent(UserEvent event){
        //Enrich the event with server-side data
        event.setEventId(UUID.randomUUID().toString());
        event.setTimestamp(Instant.now());

        //Send to Kafka
        //Key = userId -> messages from the same user always go to the same partition
        //This is important! It ensures ordering of events per user.
        CompletableFuture<SendResult<String, UserEvent>> future =
                kafkaTemplate.send(TOPIC, event.getUserId(), event);

        //handle success or failure asynchronously
        future.whenComplete((result, exception) -> {
            if(exception != null){
                log.error("failed to publish event for user {}: {}",
                        event.getUserId(), exception.getMessage());
            }else{
                log.info("Published event [{}] for user {} to partition {}",
                        event.getEventType(),
                        event.getUserId(),
                        result.getRecordMetadata().partition());
            }
        });
    }


}
