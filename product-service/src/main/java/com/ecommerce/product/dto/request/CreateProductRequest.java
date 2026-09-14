// src/main/java/com/ecommerce/product/dto/request/CreateProductRequest.java
package com.ecommerce.product.dto.request;

import com.ecommerce.product.model.enums.ProductCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 500, message = "Name must be between 2 and 500 characters")
    String name,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotBlank(message = "SKU is required")
    @Size(min = 2, max = 100, message = "SKU must be between 2 and 100 characters")
    String sku,

    @Size(max = 200, message = "Brand must not exceed 200 characters")
    String brand,

    @NotNull(message = "Category is required")
    ProductCategory category,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer and 2 decimal digits")
    BigDecimal price,

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must not be negative")
    Integer stockQuantity,

    @Size(max = 2000, message = "Image URL must not exceed 2000 characters")
    String imageUrl
) {}