// src/main/java/com/ecommerce/product/config/KafkaConsumerConfig.java
package com.ecommerce.product.config;

import com.ecommerce.product.events.avro.ProductEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Kafka consumer configuration with:
 * - Avro deserialization via Schema Registry
 * - Manual offset commit (at-least-once delivery guarantee)
 * - Retry with exponential backoff (3 attempts, 1s interval)
 * - Dead Letter Topic for permanently failed messages
 * - ErrorHandlingDeserializer to gracefully handle deserialization failures
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.properties.schema\\.registry\\.url}")
    private String schemaRegistryUrl;

    @Bean
    public ConsumerFactory<String, ProductEvent> productEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Wrap deserializers with ErrorHandlingDeserializer to catch deserialization errors
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, KafkaAvroDeserializer.class.getName());

        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, ProductEvent> productEventConsumerFactory,
                                          KafkaTemplate<String, ?> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, ProductEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productEventConsumerFactory);

        // Manual acknowledge — consumer explicitly acks after processing
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 3 concurrent consumers (matches topic partition count)
        factory.setConcurrency(3);

        // Error handler: 3 retries with 1s backoff, then publish to DLT
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(kafkaTemplate),
                new FixedBackOff(1000L, 3L));
        // Do not retry deserialization errors — send directly to DLT
        errorHandler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}