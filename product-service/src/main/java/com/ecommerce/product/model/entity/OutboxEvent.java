// src/main/java/com/ecommerce/product/model/entity/OutboxEvent.java
package com.ecommerce.product.model.entity;

import com.ecommerce.product.model.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = OutboxStatus.PENDING;
        if (this.retryCount == null) this.retryCount = 0;
    }

    public void markAsSent() {
        this.status = OutboxStatus.SENT;
        this.sentAt = Instant.now();
    }

    public void incrementRetry(String errorMsg) {
        this.retryCount++;
        this.errorMessage = errorMsg != null && errorMsg.length() > 1000
                ? errorMsg.substring(0, 1000) : errorMsg;
    }

    public void markAsFailed() {
        this.status = OutboxStatus.FAILED;
    }
}