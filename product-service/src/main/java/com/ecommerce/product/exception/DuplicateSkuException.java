// src/main/java/com/ecommerce/product/exception/DuplicateSkuException.java
package com.ecommerce.product.exception;

public class DuplicateSkuException extends BusinessException {

    public DuplicateSkuException(String sku) {
        super("DUPLICATE_SKU", "A product with SKU '" + sku + "' already exists");
    }
}