// src/main/java/com/ecommerce/product/mapper/ProductDocumentMapper.java
package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.model.document.ProductDocument;
import com.ecommerce.product.model.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductDocumentMapper {

    /**
     * MySQL entity → Elasticsearch document (write-to-read model mapping).
     */
    public ProductDocument toDocument(Product p) {
        return ProductDocument.builder()
                .productId(p.getId().toString())
                .name(p.getName())
                .description(p.getDescription())
                .sku(p.getSku())
                .brand(p.getBrand())
                .category(p.getCategory().name())
                .price(p.getPrice())
                .stockQuantity(p.getStockQuantity())
                .imageUrl(p.getImageUrl())
                .status(p.getStatus().name())
                .inStock(p.isInStock())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /**
     * Elasticsearch document → API response DTO.
     */
    public ProductResponse toResponse(ProductDocument doc) {
        return new ProductResponse(
                java.util.UUID.fromString(doc.getProductId()),
                doc.getName(), doc.getDescription(), doc.getSku(),
                doc.getBrand(),
                com.ecommerce.product.model.enums.ProductCategory.valueOf(doc.getCategory()),
                doc.getPrice(), doc.getStockQuantity(), doc.getImageUrl(),
                com.ecommerce.product.model.enums.ProductStatus.valueOf(doc.getStatus()),
                doc.isInStock(), doc.getCreatedAt(), doc.getUpdatedAt()
        );
    }
}