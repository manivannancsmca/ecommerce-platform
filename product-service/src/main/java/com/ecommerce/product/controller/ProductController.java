// src/main/java/com/ecommerce/product/controller/ProductController.java
package com.ecommerce.product.controller;

import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.ProductSearchRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.request.UpdateStockRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.dto.response.ProductSearchResponse;
import com.ecommerce.product.service.command.ProductCommandService;
import com.ecommerce.product.service.query.ProductQueryService;
import com.ecommerce.product.service.sync.ProductReindexService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Unified product API. Internally delegates to separate command and query
 * services (CQRS), but presents a single REST interface to clients.
 */
@RestController
@RequestMapping("/api/products")
@Validated
public class ProductController {

    private final ProductCommandService commandService;
    private final ProductQueryService queryService;
    private final ProductReindexService reindexService;

    public ProductController(ProductCommandService commandService,
                             ProductQueryService queryService,
                             ProductReindexService reindexService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.reindexService = reindexService;
    }

    // ── Commands (write to MySQL → outbox → Kafka → ES) ────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return commandService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateProductRequest request) {
        return commandService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse updateStock(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateStockRequest request) {
        return commandService.updateStock(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        commandService.deleteProduct(id);
    }

    // ── Queries (read from Elasticsearch, fallback to MySQL) ────────

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return queryService.getProductById(id);
    }

    @GetMapping("/search")
    public ProductSearchResponse searchProducts(ProductSearchRequest request) {
        return queryService.searchProducts(request);
    }

    // ── Admin: trigger full reindex ─────────────────────────────────

    @PostMapping("/admin/reindex")
    public Map<String, Object> reindex() throws Exception {
        ProductReindexService.ReindexResult result = reindexService.reindex();
        return Map.of(
                "status", "completed",
                "index", result.indexName(),
                "productsIndexed", result.productsIndexed());
    }
}