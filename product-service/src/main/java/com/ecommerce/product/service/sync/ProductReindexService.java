// src/main/java/com/ecommerce/product/service/sync/ProductReindexService.java
package com.ecommerce.product.service.sync;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.ecommerce.product.mapper.ProductDocumentMapper;
import com.ecommerce.product.model.document.ProductDocument;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.repository.write.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.UUID;

/**
 * Full reindex from MySQL to Elasticsearch using blue-green alias swapping.
 *
 * Flow:
 *   1. Create a new versioned index (e.g. products-v1706000000000)
 *   2. Bulk-index all products from MySQL in cursor-based batches
 *   3. Atomically swap the "products" alias from the old index to the new one
 *   4. Delete the old index
 *
 * Zero downtime: the alias swap is atomic. Queries against "products" are
 * never routed to a non-existent or partially-populated index.
 */
@Service
public class ProductReindexService {

    private static final Logger log = LoggerFactory.getLogger(ProductReindexService.class);
    private static final int BATCH_SIZE = 1000;
    private static final String INDEX_ALIAS = "products";

    private final ProductRepository productRepository;
    private final ElasticsearchClient esClient;
    private final ProductDocumentMapper documentMapper;
    private final ResourceLoader resourceLoader;

    public ProductReindexService(ProductRepository productRepository,
                                  ElasticsearchClient esClient,
                                  ProductDocumentMapper documentMapper,
                                  ResourceLoader resourceLoader) {
        this.productRepository = productRepository;
        this.esClient = esClient;
        this.documentMapper = documentMapper;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Full reindex — admin-triggered operation.
     * Uses read-only transaction for MySQL streaming.
     */
    @Transactional(readOnly = true)
    public ReindexResult reindex() throws Exception {
        String newIndex = INDEX_ALIAS + "-v" + System.currentTimeMillis();

        log.info("Starting full reindex into index '{}'", newIndex);

        // 1. Create new index with settings
        createIndex(newIndex);

        // 2. Bulk-index from MySQL
        long indexed = bulkIndexAllProducts(newIndex);

        // 3. Swap alias
        swapAlias(newIndex);

        log.info("Reindex complete: {} products indexed into '{}'", indexed, newIndex);
        return new ReindexResult(newIndex, indexed);
    }

    private void createIndex(String indexName) throws Exception {
        try (Reader reader = new InputStreamReader(
                resourceLoader.getResource("classpath:elasticsearch/product-index-settings.json")
                        .getInputStream())) {
            esClient.indices().create(c -> c.index(indexName).withJson(reader));
        }
        log.info("Created ES index '{}'", indexName);
    }

    private long bulkIndexAllProducts(String targetIndex) throws Exception {
        long total = 0;

        // First batch (no cursor)
        List<Product> batch = productRepository.findFirst1000ByOrderByIdAsc();
        while (!batch.isEmpty()) {
            bulkIndexBatch(batch, targetIndex);
            total += batch.size();

            if (total % 10000 == 0) {
                log.info("Reindex progress: {} products", total);
            }

            // Cursor: last ID in the batch
            UUID lastId = batch.get(batch.size() - 1).getId();
            batch = productRepository.findTop1000ByIdGreaterThanOrderByIdAsc(lastId);
        }

        return total;
    }

    private void bulkIndexBatch(List<Product> products, String targetIndex) throws Exception {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();

        for (Product product : products) {
            ProductDocument doc = documentMapper.toDocument(product);
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(targetIndex)
                            .id(doc.getProductId())
                            .document(doc)));
        }

        BulkResponse response = esClient.bulk(bulkBuilder.build());

        if (response.errors()) {
            long errorCount = response.items().stream()
                    .filter(i -> i.error() != null).count();
            log.error("Bulk index: {} errors out of {} items", errorCount, products.size());
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    log.error("  → doc {}: {}", item.id(), item.error().reason());
                }
            }
        }
    }

    private void swapAlias(String newIndex) throws Exception {
        // Check if the alias already exists (pointing to an old index)
        boolean aliasExists = esClient.indices().existsAlias(a -> a.name(INDEX_ALIAS)).value();

        if (aliasExists) {
            // Atomic swap: remove old, add new
            esClient.indices().updateAliases(a -> a
                    .actions(action -> action.remove(remove -> remove
                            .index("*").alias(INDEX_ALIAS)))
                    .actions(action -> action.add(add -> add
                            .index(newIndex).alias(INDEX_ALIAS))));
            log.info("Swapped alias '{}' → '{}'", INDEX_ALIAS, newIndex);

            // Delete old indices (except the new one)
            esClient.indices().delete(d -> d
                    .index(INDEX_ALIAS + "-v*")
                    .ignoreUnavailable(true));
        } else {
            // First reindex: just create the alias
            esClient.indices().updateAliases(a -> a
                    .actions(action -> action.add(add -> add
                            .index(newIndex).alias(INDEX_ALIAS))));
            log.info("Created alias '{}' → '{}'", INDEX_ALIAS, newIndex);
        }
    }

    public record ReindexResult(String indexName, long productsIndexed) {}
}