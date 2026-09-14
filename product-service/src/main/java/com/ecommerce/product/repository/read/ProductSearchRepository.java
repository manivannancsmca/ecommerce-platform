// src/main/java/com/ecommerce/product/repository/read/ProductSearchRepository.java
package com.ecommerce.product.repository.read;

import com.ecommerce.product.model.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    // Basic CRUD is inherited (save, findById, deleteById, existsById, count).
    // Complex search queries are built programmatically in ProductQueryServiceImpl.
}