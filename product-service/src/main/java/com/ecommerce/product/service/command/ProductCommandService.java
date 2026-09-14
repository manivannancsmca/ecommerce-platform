// src/main/java/com/ecommerce/product/service/command/ProductCommandService.java
package com.ecommerce.product.service.command;

import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.request.UpdateStockRequest;
import com.ecommerce.product.dto.response.ProductResponse;

import java.util.UUID;

public interface ProductCommandService {
    ProductResponse createProduct(CreateProductRequest request);
    ProductResponse updateProduct(UUID id, UpdateProductRequest request);
    ProductResponse updateStock(UUID id, UpdateStockRequest request);
    void deleteProduct(UUID id);
}