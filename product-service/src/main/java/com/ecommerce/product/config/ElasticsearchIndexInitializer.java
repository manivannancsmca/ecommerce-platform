// src/main/java/com/ecommerce/product/config/ElasticsearchIndexInitializer.java
package com.ecommerce.product.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Ensures the "products" Elasticsearch index exists on application startup.
 * If the index does not exist, creates it with the settings and mappings
 * defined in product-index-settings.json.
 *
 * For zero-downtime reindexing, use ProductReindexService which creates a
 * new versioned index and atomically swaps the alias.
 */
@Component
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexInitializer.class);
    private static final String INDEX_ALIAS = "products";

    private final ElasticsearchClient client;
    private final ResourceLoader resourceLoader;

    public ElasticsearchIndexInitializer(ElasticsearchClient client, ResourceLoader resourceLoader) {
        this.client = client;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        boolean exists = client.indices().exists(e -> e.index(INDEX_ALIAS)).value();

        if (!exists) {
            log.info("Elasticsearch index '{}' not found; creating...", INDEX_ALIAS);
            try (Reader reader = new InputStreamReader(
                    resourceLoader.getResource("classpath:elasticsearch/product-index-settings.json")
                            .getInputStream())) {
                client.indices().create(c -> c.index(INDEX_ALIAS).withJson(reader));
            }
            log.info("Elasticsearch index '{}' created successfully", INDEX_ALIAS);
        } else {
            log.info("Elasticsearch index '{}' already exists", INDEX_ALIAS);
        }
    }
}