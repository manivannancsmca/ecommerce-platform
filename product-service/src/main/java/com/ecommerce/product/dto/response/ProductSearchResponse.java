// src/main/java/com/ecommerce/product/dto/response/ProductSearchResponse.java
package com.ecommerce.product.dto.response;

import java.util.List;

public record ProductSearchResponse(
    List<ProductResponse> products,
    long totalElements,
    int currentPage,
    int pageSize,
    int totalPages,
    boolean hasNext,
    /** Base64-encoded cursor for the next page. Null when hasNext is false. */
    String nextSearchAfter
) {}