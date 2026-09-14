-- src/main/resources/db/migration/V2__create_outbox_events_table.sql

CREATE TABLE outbox_events (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    event_id        VARCHAR(36)    NOT NULL,
    aggregate_type  VARCHAR(50)    NOT NULL,
    aggregate_id    VARCHAR(36)    NOT NULL,
    event_type      VARCHAR(50)    NOT NULL,
    payload         JSON           NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at         DATETIME(6)    DEFAULT NULL,
    retry_count     INT            NOT NULL DEFAULT 0,
    error_message   VARCHAR(1000)  DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    INDEX idx_outbox_status_created (status, created_at),
    INDEX idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;