// src/main/java/com/ecommerce/product/service/sync/KafkaProductEventPublisher.java
package com.ecommerce.product.service.sync;

import com.ecommerce.product.common.CorrelationIdHolder;
import com.ecommerce.product.events.avro.ProductEvent;
import com.ecommerce.product.model.entity.OutboxEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Converts OutboxEvent (JSON in MySQL) → ProductEvent (Avro) and publishes
 * to the product-events Kafka topic.
 *
 * Uses the product ID as the partition key so all events for the same product
 * land in the same partition and are consumed in order.
 */
@Component
public class KafkaProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaProductEventPublisher.class);
    private static final String TOPIC = "product-events";
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProductEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OutboxEvent outboxEvent) {
        ProductEvent avroEvent = ProductEvent.newBuilder()
                .setEventId(outboxEvent.getEventId())
                .setEventType(outboxEvent.getEventType())
                .setProductId(outboxEvent.getAggregateId())
                .setTimestamp(outboxEvent.getCreatedAt().toEpochMilli())
                .setVersion(1)
                .setPayload(outboxEvent.getPayload())
                .setCorrelationId(CorrelationIdHolder.getCorrelationId())
                .build();

        // Partition key = productId (ensures ordering per product)
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                TOPIC, null, outboxEvent.getAggregateId(), avroEvent);

        // Add correlation ID as a Kafka header for downstream propagation
        String correlationId = CorrelationIdHolder.getCorrelationId();
        if (correlationId != null) {
            record.headers().add(new RecordHeader(
                    "X-Correlation-Id", correlationId.getBytes(StandardCharsets.UTF_8)));
        }

        try {
            kafkaTemplate.send(record).get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new KafkaPublishException(
                    "Failed to publish event " + outboxEvent.getEventId() + " to topic " + TOPIC, e);
        }
    }

    /**
     * Checked exception wrapper for Kafka publish failures.
     * Caught by OutboxPublisher for retry handling.
     */
    public static class KafkaPublishException extends RuntimeException {
        public KafkaPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}