-- src/main/resources/db/migration/V1__create_users_table.sql

CREATE TABLE users (
    id              BINARY(16)     NOT NULL,
    email           VARCHAR(255)   NOT NULL,
    password_hash   VARCHAR(255)   NOT NULL,
    first_name      VARCHAR(100)   NOT NULL,
    last_name       VARCHAR(100)   NOT NULL,
    phone           VARCHAR(20)    DEFAULT NULL,
    role            VARCHAR(20)    NOT NULL DEFAULT 'CUSTOMER',
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_status (status),
    INDEX idx_users_role (role),
    INDEX idx_users_created_at (created_at),
    INDEX idx_users_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;