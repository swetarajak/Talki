// trending-service/src/main/java/com/shoppingfeed/trending/streams/TrendingProductsTopology.java
package com.shoppingfeed.trending.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shoppingfeed.common.event.UserEvent;
import com.shoppingfeed.trending.service.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingProductsTopology {

    private final TrendingService trendingService;
    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    // Window size — count events in the last 30 minutes
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(30);

    // Advance — slide the window every 5 minutes
    // So we get fresh trending results every 5 minutes
    private static final Duration ADVANCE_BY = Duration.ofMinutes(5);

    /**
     * @Autowired on a method with StreamsBuilder parameter —
     * Spring calls this method automatically and passes the builder.
     * We use the builder to define our stream processing pipeline.
     *
     * The pipeline is like a series of pipes:
     * Source topic → filter → group → window → count → sink
     */
    @Autowired
    public void buildTopology(StreamsBuilder streamsBuilder) {

        // Step 1: Read from the "user-events" topic
        // This creates a KStream — an infinite stream of events
        KStream<String, String> rawStream = streamsBuilder.stream(
                "user-events",
                Consumed.with(Serdes.String(), Serdes.String())
        );

        // Step 2: Parse the raw JSON string into UserEvent objects,
        //         then filter to only keep meaningful events
        //         (skip SCROLL events — too noisy)
        rawStream
                .mapValues(this::parseEvent)        // JSON string → UserEvent
                .filter((key, event) ->             // remove nulls and scrolls
                        event != null &&
                                event.getProductId() != null &&
                                event.getEventType() != UserEvent.EventType.SCROLL)

                // Step 3: Re-key by productId
                // Original key = userId (set by ingestion service)
                // We need key = productId (to count per product)
                .selectKey((userId, event) -> event.getProductId())

                // Step 4: Give each event a "weight" based on type
                // Purchase = 5 points, Add to cart = 3, Click = 2, View = 1
                .mapValues(this::getEventWeight)    // UserEvent → weight string

                // Step 5: Group all events by productId
                .groupByKey()

                // Step 6: Apply sliding window — count events per product
                //         in the last 30 minutes, updating every 5 minutes
                .windowedBy(
                        TimeWindows.ofSizeWithNoGrace(WINDOW_SIZE)
                                .advanceBy(ADVANCE_BY)
                )

                // Step 7: Sum up the weights (not just count)
                // A product with 1 purchase ranks higher than one with 5 views
                .reduce((weight1, weight2) -> {
                    double sum = Double.parseDouble(weight1)
                            + Double.parseDouble(weight2);
                    return String.valueOf(sum);
                })

                // Step 8: Convert windowed result to a regular stream
                .toStream()

                // Step 9: For each product's updated score,
                //         save it to Redis
                .foreach(this::saveToRedis);

        log.info("Trending products topology built successfully");
    }

    /**
     * Parses a JSON string into a UserEvent.
     * Returns null if parsing fails — the filter step above removes nulls.
     */
    private UserEvent parseEvent(String json) {
        try {
            return objectMapper.readValue(json, UserEvent.class);
        } catch (Exception e) {
            log.warn("Failed to parse event JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Assigns a weight to each event type.
     * Purchase signals much stronger interest than a view.
     *
     * Returns a String because Kafka Streams stores values as Strings here.
     */
    private String getEventWeight(UserEvent event) {
        return switch (event.getEventType()) {
            case PURCHASE      -> "5.0";
            case ADD_TO_CART   -> "3.0";
            case CLICK         -> "2.0";
            case VIEW          -> String.valueOf(
                    1.0 + (event.getDwellTimeSeconds() / 100.0));
            default            -> "0.5";
        };
    }

    /**
     * Saves the trending score to Redis sorted set.
     *
     * Redis sorted sets are perfect for leaderboards/rankings:
     *   ZADD trending:products <score> <productId>
     *
     * We can then do:
     *   ZREVRANGE trending:products 0 9  → top 10 trending products
     */
    private void saveToRedis(Windowed<String> windowedKey, String score) {
        try {
            String productId = windowedKey.key();
            double trendingScore = Double.parseDouble(score);
            trendingService.updateTrendingScore(productId, trendingScore);
        } catch (Exception e) {
            log.error("Failed to save trending score: {}", e.getMessage());
        }
    }
}