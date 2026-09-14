// src/main/java/com/ecommerce/product/exception/ProductNotFoundException.java
package com.ecommerce.product.exception;

import java.util.UUID;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(UUID id) {
        super("PRODUCT_NOT_FOUND", "Product not found with id: " + id);
    }

    public ProductNotFoundException(String sku) {
        super("PRODUCT_NOT_FOUND", "Product not found with SKU: " + sku);
    }
}