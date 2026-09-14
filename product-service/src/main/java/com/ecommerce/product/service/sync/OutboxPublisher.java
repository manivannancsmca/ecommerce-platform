// src/main/java/com/ecommerce/product/service/sync/OutboxPublisher.java
package com.ecommerce.product.service.sync;

import com.ecommerce.product.model.entity.OutboxEvent;
import com.ecommerce.product.repository.write.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls the outbox table for pending events and publishes them to Kafka.
 * Uses SELECT ... FOR UPDATE SKIP LOCKED so multiple instances can
 * process different batches concurrently without conflict.
 *
 * Guarantees at-least-once delivery: if the service crashes after Kafka
 * publish but before the outbox status update, the event is re-published
 * on restart. The consumer (ProductEventConsumer) must be idempotent.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxRepository;
    private final KafkaProductEventPublisher eventPublisher;

    @Value("${outbox.batch-size:20}")
    private int batchSize;

    @Value("${outbox.max-retries:5}")
    private int maxRetries;

    public OutboxPublisher(OutboxEventRepository outboxRepository,
                           KafkaProductEventPublisher eventPublisher) {
        this.outboxRepository = outboxRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findPendingEvents(batchSize, maxRetries);

        if (events.isEmpty()) return;

        log.debug("Processing {} outbox events", events.size());

        for (OutboxEvent event : events) {
            try {
                eventPublisher.publish(event);
                event.markAsSent();
                log.debug("Published outbox event {} ({})", event.getEventId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {} (attempt {}): {}",
                        event.getEventId(), event.getRetryCount() + 1, e.getMessage());
                event.incrementRetry(e.getMessage());
                if (event.getRetryCount() >= maxRetries) {
                    event.markAsFailed();
                    log.error("Outbox event {} permanently failed after {} retries",
                            event.getEventId(), maxRetries);
                }
            }
        }

        outboxRepository.saveAll(events);
    }
}