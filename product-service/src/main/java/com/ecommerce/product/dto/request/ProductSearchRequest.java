// src/main/java/com/ecommerce/product/dto/request/ProductSearchRequest.java
package com.ecommerce.product.dto.request;

import java.math.BigDecimal;

/**
 * Search/filter criteria for product queries.
 * Bound from query parameters via @ModelAttribute (no @RequestBody).
 * All fields are optional; null means "no filter on this dimension".
 */
public record ProductSearchRequest(
    String query,
    String category,
    String brand,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean inStock,
    String sortBy,
    String sortDirection,
    Integer page,
    Integer size,
    String searchAfter
) {}