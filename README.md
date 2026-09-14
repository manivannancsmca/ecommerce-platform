# Product Service — CQRS with MySQL Write Model + Elasticsearch Read Model

## Architecture Overview

```plaintext
                ┌───────────────────────────────────────────────┐
                │              Product Service                  │
                │                                               │
┌─────────┐     │ ┌─────────────────┐ ┌──────────────────┐    │
│  Client │────►│ │ProductController│ │OutboxPublisher │    │
└─────────┘     │ │ (commands +     │ │  @Scheduled    │    │
                │ │ queries)        │ └──────────────────┘    │
                │ └─────────┬─────────┘                        │
                │           │                                  │
                │   ┌───────▼────────┐                        │
                │   │ Command Service │                        │
                │   └───────┬────────┘                        │
                │           │                                 │
                │   ┌───────▼────────┐                        │
                │   │ Query Service  │                        │
                │   └───────┬────────┘                        │
                │           │                                 │
                │   ┌───────▼────────┐                        │
                │   │ KafkaEventPublisher                     │
                │   └─────────┬───────┘                        │
                │             │                                │
                │ ┌───────────▼───────────┐                   │
                │ │ Reads Kafka → Writes │                   │
                │ │ to Elasticsearch     │                   │
                │ └───────────────────────┘                   │
                │                                             │
                │            Write Path (Commands)            │
                │   Client → Controller → CommandService →  │
                │   MySQL + Outbox (single transaction)     │
                │   → OutboxPublisher polls → Kafka          │
                │                                             │
                │            Read Path (Queries)              │
                │   Client → Controller → QueryService →     │
                │   Elasticsearch                           │
                └─────────────────────────────────────────────┘

product-service/
├── pom.xml
└── src/main/
    ├── avro/
    │   └── product-event.avsc
    ├── resources/
    │   ├── application.yml
    │   ├── elasticsearch/
    │   │   └── product-index-settings.json
    │   └── db/migration/
    │       ├── V1__create_products_table.sql
    │       └── V2__create_outbox_events_table.sql
    └── java/com/ecommerce/product/
        ├── ProductServiceApplication.java
        ├── model/
        │   ├── enums/
        │   │   ├── ProductStatus.java
        │   │   ├── ProductCategory.java
        │   │   └── OutboxStatus.java
        │   ├── entity/
        │   │   ├── Product.java
        │   │   └── OutboxEvent.java
        │   └── document/
        │       └── ProductDocument.java
        ├── dto/
        │   ├── request/
        │   │   ├── CreateProductRequest.java
        │   │   ├── UpdateProductRequest.java
        │   │   ├── UpdateStockRequest.java
        │   │   └── ProductSearchRequest.java
        │   └── response/
        │       ├── ProductResponse.java
        │       ├── ProductSearchResponse.java
        │       └── ErrorResponse.java
        ├── mapper/
        │   ├── ProductMapper.java
        │   └── ProductDocumentMapper.java
        ├── repository/
        │   ├── write/
        │   │   ├── ProductRepository.java
        │   │   └── OutboxEventRepository.java
        │   └── read/
        │       └── ProductSearchRepository.java
        ├── exception/
        │   ├── BusinessException.java
        │   ├── ProductNotFoundException.java
        │   ├── InsufficientStockException.java
        │   ├── DuplicateSkuException.java
        │   └── GlobalExceptionHandler.java
        ├── service/
        │   ├── command/
        │   │   ├── ProductCommandService.java
        │   │   └── impl/
        │   │       └── ProductCommandServiceImpl.java
        │   ├── query/
        │   │   ├── ProductQueryService.java
        │   │   └── impl/
        │   │       └── ProductQueryServiceImpl.java
        │   └── sync/
        │       ├── OutboxPublisher.java
        │       ├── KafkaProductEventPublisher.java
        │       ├── ProductEventConsumer.java
        │       └── ProductReindexService.java
        ├── config/
        │   ├── KafkaConsumerConfig.java
        │   └── ElasticsearchIndexInitializer.java
        ├── controller/
        │   └── ProductController.java
        ├── common/
        │   └── CorrelationIdHolder.java
        ├── filter/
        │   └── CorrelationIdFilter.java
        └── aspect/
            └── LoggingAspect.java
