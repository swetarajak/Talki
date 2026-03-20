package com.shoppingfeed.ingestion.config;


import com.shoppingfeed.common.event.UserEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, UserEvent> producerFactory(){
        Map<String, Object> config = new HashMap<>();

        //Where is Kafka running?
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        //Key Serializer: the "key" of each message is a string(we'll use userId)
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        //Value Serializer: the "value" is our UserEvent object - convert it to JSON
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        //Reliability setting: wait for kafka to confirm message was received
        config.put(ProducerConfig.ACKS_CONFIG, "all");

        return new DefaultKafkaProducerFactory<>(config);
    }

    /*
     * KafkaTemplate is the main class we use to actually send messages.
     * Think of it like JdbcTemplate but for Kafka instead of SQL.
     */
    @Bean
    public KafkaTemplate<String, UserEvent> kafkaTemplate(){
        return new KafkaTemplate<>(producerFactory());
    }

}
