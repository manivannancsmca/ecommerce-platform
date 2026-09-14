-- src/main/resources/db/migration/V1__create_products_table.sql

CREATE TABLE products (
    id              BINARY(16)     NOT NULL,
    name            VARCHAR(500)   NOT NULL,
    description     TEXT,
    sku             VARCHAR(100)   NOT NULL,
    brand           VARCHAR(200),
    category        VARCHAR(50)    NOT NULL,
    price           DECIMAL(12,2)  NOT NULL,
    stock_quantity  INT            NOT NULL DEFAULT 0,
    image_url       VARCHAR(2000),
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    INDEX idx_products_category_status (category, status),
    INDEX idx_products_status (status),
    INDEX idx_products_brand (brand),
    INDEX idx_products_price (price),
    INDEX idx_products_created_at (created_at),
    INDEX idx_products_stock (stock_quantity),
    FULLTEXT INDEX ft_products_name_desc (name, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;