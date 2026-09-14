// src/main/java/com/ecommerce/product/exception/InsufficientStockException.java
package com.ecommerce.product.exception;

import java.util.UUID;

public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(UUID productId, int requested, int available) {
        super("INSUFFICIENT_STOCK",
                String.format("Insufficient stock for product %s: requested=%d, available=%d",
                        productId, requested, available));
    }
}