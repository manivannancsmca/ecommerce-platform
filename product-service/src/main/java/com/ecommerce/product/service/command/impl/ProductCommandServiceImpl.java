// src/main/java/com/ecommerce/product/service/command/impl/ProductCommandServiceImpl.java
package com.ecommerce.product.service.command.impl;

import com.ecommerce.product.common.CorrelationIdHolder;
import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.request.UpdateStockRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.exception.DuplicateSkuException;
import com.ecommerce.product.exception.InsufficientStockException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.mapper.ProductMapper;
import com.ecommerce.product.model.entity.OutboxEvent;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.model.enums.OutboxStatus;
import com.ecommerce.product.model.enums.ProductStatus;
import com.ecommerce.product.repository.write.OutboxEventRepository;
import com.ecommerce.product.repository.write.ProductRepository;
import com.ecommerce.product.service.command.ProductCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProductCommandServiceImpl implements ProductCommandService {

    private static final Logger log = LoggerFactory.getLogger(ProductCommandServiceImpl.class);
    private static final String AGGREGATE_TYPE = "PRODUCT";

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxRepository;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public ProductCommandServiceImpl(ProductRepository productRepository,
                                     OutboxEventRepository outboxRepository,
                                     ProductMapper productMapper,
                                     ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.outboxRepository = outboxRepository;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        String normalizedSku = request.sku().toUpperCase().trim();

        if (productRepository.existsBySku(normalizedSku)) {
            throw new DuplicateSkuException(normalizedSku);
        }

        Product product = productMapper.toEntity(request);
        product.setId(UUID.randomUUID());
        product.setSku(normalizedSku);
        product.setStatus(ProductStatus.ACTIVE);

        Product saved = productRepository.save(product);

        // Publish event via transactional outbox — committed in the same transaction
        saveOutboxEvent(saved.getId().toString(), "PRODUCT_CREATED",
                buildPayload("product", saved));

        log.info("Created product {} with SKU '{}'", saved.getId(), saved.getSku());
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = findProductOrThrow(id);

        String oldPrice = product.getPrice().toPlainString();
        productMapper.updateEntity(product, request);

        Product saved = productRepository.save(product);

        // Determine if price changed for a dedicated PRICE_CHANGED event
        String newPrice = saved.getPrice().toPlainString();
        String eventType = oldPrice.equals(newPrice) ? "PRODUCT_UPDATED" : "PRODUCT_UPDATED";

        saveOutboxEvent(id.toString(), eventType, buildPayload("product", saved));

        log.info("Updated product {}", id);
        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse updateStock(UUID id, UpdateStockRequest request) {
        Product product = findProductOrThrow(id);
        int previousQuantity = product.getStockQuantity();

        if (request.increment()) {
            product.incrementStock(request.quantity());
        } else {
            if (product.getStockQuantity() < request.quantity()) {
                throw new InsufficientStockException(id, request.quantity(), product.getStockQuantity());
            }
            product.decrementStock(request.quantity());
        }

        // Auto-update status based on stock level
        if (product.getStockQuantity() == 0 && product.getStatus() == ProductStatus.ACTIVE) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStockQuantity() > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product saved = productRepository.save(product);

        saveOutboxEvent(id.toString(), "STOCK_UPDATED",
                buildStockPayload(previousQuantity, saved.getStockQuantity(),
                        request.increment() ? request.quantity() : -request.quantity()));

        log.info("Product {} stock changed: {} → {}", id, previousQuantity, saved.getStockQuantity());
        return productMapper.toResponse(saved);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = findProductOrThrow(id);
        product.setStatus(ProductStatus.DISCONTINUED);
        productRepository.save(product);

        saveOutboxEvent(id.toString(), "PRODUCT_DELETED", null);
        log.info("Soft-deleted (discontinued) product {}", id);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /**
     * Writes an event to the outbox table within the current transaction.
     * The OutboxPublisher (scheduled task) will pick it up and publish to Kafka.
     */
    private void saveOutboxEvent(String aggregateId, String eventType, String payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        outboxRepository.save(event);
    }

    private String buildPayload(String key, Object value) {
        try {
            return objectMapper.writeValueAsString(Map.of(key, value));
        } catch (Exception e) {
            log.error("Failed to serialize outbox payload", e);
            return "{}";
        }
    }

    private String buildStockPayload(int previousQuantity, int newQuantity, int delta) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "previousQuantity", previousQuantity,
                    "newQuantity", newQuantity,
                    "delta", delta));
        } catch (Exception e) {
            log.error("Failed to serialize stock payload", e);
            return "{}";
        }
    }
}