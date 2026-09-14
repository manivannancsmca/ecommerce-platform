// src/main/java/com/ecommerce/product/service/sync/ProductEventConsumer.java
package com.ecommerce.product.service.sync;

import com.ecommerce.product.events.avro.ProductEvent;
import com.ecommerce.product.mapper.ProductDocumentMapper;
import com.ecommerce.product.model.document.ProductDocument;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.repository.read.ProductSearchRepository;
import com.ecommerce.product.repository.write.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Consumes product events from Kafka and synchronises Elasticsearch.
 * Always fetches the latest state from MySQL (source of truth) rather than
 * trusting the event payload, which guarantees consistency regardless of
 * event ordering or delays.
 *
 * Idempotent: re-processing the same event produces the same ES state.
 */
@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ProductRepository productRepository;
    private final ProductSearchRepository searchRepository;
    private final ProductDocumentMapper documentMapper;

    public ProductEventConsumer(ProductRepository productRepository,
                                ProductSearchRepository searchRepository,
                                ProductDocumentMapper documentMapper) {
        this.productRepository = productRepository;
        this.searchRepository = searchRepository;
        this.documentMapper = documentMapper;
    }

    @KafkaListener(
            topics = "product-events",
            groupId = "product-service-es-sync",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleProductEvent(
            @Payload ProductEvent event,
            @Header(KafkaHeaders.ACKNOWLEDGMENT) Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        String eventType = event.getEventType();
        String productId = event.getProductId();

        log.info("Received event: type={}, productId={}, topic={}, partition={}, offset={}",
                eventType, productId, topic, partition, offset);

        try {
            switch (eventType) {
                case "PRODUCT_CREATED", "PRODUCT_UPDATED", "STOCK_UPDATED", "PRICE_CHANGED" ->
                        syncProductToEs(productId);
                case "PRODUCT_DELETED" ->
                        deleteProductFromEs(productId);
                default ->
                        log.warn("Unknown event type '{}' for product {}; skipping", eventType, productId);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process event {} for product {}: {}",
                    event.getEventId(), productId, e.getMessage(), e);
            throw e; // Let the DefaultErrorHandler handle retries / DLT
        }
    }

    /**
     * Fetches the product from MySQL (source of truth) and upserts into ES.
     * If the product no longer exists in MySQL (e.g., hard-deleted), removes
     * it from ES instead.
     */
    private void syncProductToEs(String productId) {
        UUID id = UUID.fromString(productId);
        Optional<Product> productOpt = productRepository.findById(id);

        if (productOpt.isPresent()) {
            ProductDocument doc = documentMapper.toDocument(productOpt.get());
            searchRepository.save(doc);
            log.debug("Synced product {} to Elasticsearch", productId);
        } else {
            log.warn("Product {} not found in MySQL; removing from ES if present", productId);
            searchRepository.deleteById(productId);
        }
    }

    private void deleteProductFromEs(String productId) {
        searchRepository.deleteById(productId);
        log.debug("Removed product {} from Elasticsearch", productId);
    }
}