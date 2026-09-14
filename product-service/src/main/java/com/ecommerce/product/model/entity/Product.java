// src/main/java/com/ecommerce/product/model/entity/Product.java
package com.ecommerce.product.model.entity;

import com.ecommerce.product.model.enums.ProductCategory;
import com.ecommerce.product.model.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(length = 200)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(name = "image_url", length = 2000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    /**
     * Optimistic locking — prevents lost updates when two requests modify the
     * same product concurrently. JPA checks this version on UPDATE; if the
     * database version is higher than the entity version, an
     * OptimisticLockException is thrown.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Domain method — encapsulates the business rule for stock decrement.
     * Throws if insufficient stock rather than allowing negative values.
     */
    public void decrementStock(int quantity) {
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException(
                "Insufficient stock for product " + id +
                ": requested=" + quantity + ", available=" + stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public void incrementStock(int quantity) {
        this.stockQuantity += quantity;
    }

    public boolean isInStock() {
        return this.stockQuantity != null && this.stockQuantity > 0;
    }
}