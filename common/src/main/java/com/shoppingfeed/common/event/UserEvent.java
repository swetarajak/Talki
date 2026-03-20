package com.shoppingfeed.common.event;

import jdk.jfr.DataAmount;
import jdk.jfr.EventType;
import lombok.*;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
    private String eventId;
    private String userId;
    private String productId;
    private String sessionId;

    private EventType eventType;

    private long dwellTimeSeconds;
    private Instant timestamp;

    private String deviceType;
    private String locale;

    public enum EventType{
        VIEW,
        CLICK,
        ADD_TO_CART,
        REMOVE_FROM_CART,
        PURCHASE,
        SEARCH,
        SCROLL
    }
}
