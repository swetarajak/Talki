// event-ingestion-service/src/main/java/com/shoppingfeed/ingestion/controller/EventController.java
package com.shoppingfeed.ingestion.controller;

import com.shoppingfeed.common.event.UserEvent;
import com.shoppingfeed.ingestion.service.EventPublisherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventPublisherService publisherService;

    /**
     * Single event endpoint.
     * The app calls this every time a user does one action.
     *
     * POST /api/v1/events
     * Body: { "userId": "u123", "productId": "p456", "eventType": "VIEW", ... }
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> trackEvent(
            @Valid @RequestBody UserEvent event) {

        log.info("Received event: {} from user: {}", event.getEventType(), event.getUserId());
        publisherService.publishEvent(event);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "status", "accepted",
                        "message", "Event queued for processing"
                ));
    }

    /**
     * Batch event endpoint.
     * Mobile apps often buffer events and send them in bulk to save battery.
     *
     * POST /api/v1/events/batch
     * Body: [ { event1 }, { event2 }, ... ]
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> trackEvents(
            @Valid @RequestBody List<UserEvent> events) {

        log.info("Received batch of {} events", events.size());
        events.forEach(publisherService::publishEvent);

        return ResponseEntity.accepted()
                .body(Map.of(
                        "status", "accepted",
                        "processed", events.size()
                ));
    }

    /**
     * Health check — useful to verify the service is running
     * GET /api/v1/events/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}