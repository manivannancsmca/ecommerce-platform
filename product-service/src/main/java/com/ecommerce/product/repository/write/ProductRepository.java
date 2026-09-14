// src/main/java/com/ecommerce/product/repository/write/ProductRepository.java
package com.ecommerce.product.repository.write;

import com.ecommerce.product.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM Product p WHERE p.sku = :sku AND p.id <> :excludeId")
    boolean existsBySkuAndIdNot(@Param("sku") String sku, @Param("excludeId") UUID excludeId);

    /**
     * Cursor-based fetch for bulk reindexing. Returns products with id > cursor,
     * ordered by id, limited to 1000 rows. Uses the clustered primary key index.
     */
    List<Product> findTop1000ByIdGreaterThanOrderByIdAsc(UUID id);

    List<Product> findFirst1000ByOrderByIdAsc();
}