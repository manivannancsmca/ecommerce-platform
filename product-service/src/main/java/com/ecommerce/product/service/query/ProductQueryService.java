// src/main/java/com/ecommerce/product/service/query/ProductQueryService.java
package com.ecommerce.product.service.query;

import com.ecommerce.product.dto.request.ProductSearchRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.dto.response.ProductSearchResponse;

import java.util.UUID;

public interface ProductQueryService {
    ProductResponse getProductById(UUID id);
    ProductSearchResponse searchProducts(ProductSearchRequest request);
}