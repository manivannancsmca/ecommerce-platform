// src/main/java/com/ecommerce/product/mapper/ProductMapper.java
package com.ecommerce.product.mapper;

import com.ecommerce.product.dto.request.CreateProductRequest;
import com.ecommerce.product.dto.request.UpdateProductRequest;
import com.ecommerce.product.dto.response.ProductResponse;
import com.ecommerce.product.model.entity.Product;
import com.ecommerce.product.model.enums.ProductStatus;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(CreateProductRequest req) {
        return Product.builder()
                .name(req.name())
                .description(req.description())
                .sku(req.sku())
                .brand(req.brand())
                .category(req.category())
                .price(req.price())
                .stockQuantity(req.stockQuantity())
                .imageUrl(req.imageUrl())
                .build();
    }

    public ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getSku(),
                p.getBrand(), p.getCategory(), p.getPrice(), p.getStockQuantity(),
                p.getImageUrl(), p.getStatus(), p.isInStock(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    public void updateEntity(Product product, UpdateProductRequest req) {
        product.setName(req.name());
        product.setDescription(req.description());
        product.setBrand(req.brand());
        product.setCategory(req.category());
        product.setPrice(req.price());
        if (req.imageUrl() != null) product.setImageUrl(req.imageUrl());
        if (req.status() != null) product.setStatus(req.status());
    }
}