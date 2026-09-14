// src/main/java/com/ecommerce/product/repository/write/OutboxEventRepository.java
package com.ecommerce.product.repository.write;

import com.ecommerce.product.model.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Select pending events with row-level locking.
     * FOR UPDATE SKIP LOCKED allows multiple processor instances to work on
     * different batches without blocking each other (PostgreSQL / MySQL 8.0+).
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE status = 'PENDING'
          AND retry_count < :maxRetries
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(
            @Param("limit") int limit,
            @Param("maxRetries") int maxRetries);

    long countByStatus(String status);
}