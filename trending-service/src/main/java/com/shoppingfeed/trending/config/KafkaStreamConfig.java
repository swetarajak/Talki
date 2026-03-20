// trending-service/src/main/java/com/shoppingfeed/trending/config/KafkaStreamsConfig.java
package com.shoppingfeed.trending.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * @EnableKafkaStreams — tells Spring to activate Kafka Streams support.
 * Without this annotation, Spring won't know to start the streams topology.
 */
@Configuration
@EnableKafkaStreams
public class KafkaStreamConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * This bean MUST be named exactly
     * "defaultKafkaStreamsConfig" — Spring looks for this
     * specific name to configure Kafka Streams.
     */
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();

        // Where is Kafka?
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Unique name for this streams application
        // Kafka uses this to track processing progress
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "trending-streams-app");

        // Default serializer/deserializer for keys (String)
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass());

        // Default serializer/deserializer for values (String)
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass());

        // Start reading from beginning if no offset exists
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // How often to commit progress (every 1 second)
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

        return new KafkaStreamsConfiguration(props);
    }
}