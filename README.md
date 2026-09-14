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
