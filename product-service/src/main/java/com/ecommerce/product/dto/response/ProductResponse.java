// src/main/java/com/ecommerce/product/dto/response/ProductResponse.java
package com.ecommerce.product.dto.response;

import com.ecommerce.product.model.enums.ProductCategory;
import com.ecommerce.product.model.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    String sku,
    String brand,
    ProductCategory category,
    BigDecimal price,
    Integer stockQuantity,
    String imageUrl,
    ProductStatus status,
    boolean inStock,
    Instant createdAt,
    Instant updatedAt
) {}