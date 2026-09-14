Product Service — CQRS with MySQL Write Model + Elasticsearch Read Model

                    ┌───────────────────────────────────────────────┐
                    │              Product Service                  │
                    │                                               │
 ┌─────────┐       │  ┌─────────────────┐    ┌──────────────────┐  │
 │  Client  │──────►│  │ProductController│    │OutboxPublisher   │  │
 │  /Order  │  REST │  │  (commands +    │    │  @Scheduled      │  │
 │  Service │       │  │   queries)      │    │  poll outbox     │  │
 └─────────┘       │  └───┬─────────┬───┘    └───┬──────────────┘  │
                    │      │         │            │                 │
                    │  ┌───▼───┐ ┌───▼────┐  ┌───▼──────────────┐  │
                    │  │Command│ │ Query  │  │KafkaEventPublisher│  │
                    │  │Service│ │Service │  └───┬──────────────┘  │
                    │  └───┬───┘ └───┬────┘      │                 │
                    └──────┼─────────┼───────────┼─────────────────┘
                           │         │           │
                    ┌──────▼───┐ ┌───▼──────┐ ┌──▼──────────┐
                    │  MySQL   │ │Elastic-  │ │   Kafka     │
                    │product_db│ │search    │ │(3 brokers)  │
                    │(write)   │ │(read)    │ │product-events│
                    └──────┬───┘ └──▲───────┘ └──┬──────────┘
                           │        │            │
                           │  ┌─────┴────────────▼──┐
                           │  │ProductEventConsumer  │
                           │  │reads Kafka → writes  │
                           │  │to Elasticsearch      │
                           │  └──────────────────────┘
                           │
                    ┌──────▼───────┐
                    │outbox_events │
                    │(transactional│
                    │ outbox)      │
                    └──────────────┘
---

Write path (Command): Client → Controller → CommandService → MySQL + outbox (single transaction) → OutboxPublisher polls → Kafka

Read path (Query): Client → Controller → QueryService → Elasticsearch

Sync path: Kafka → ProductEventConsumer → fetch from MySQL → write to Elasticsearch

---

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
