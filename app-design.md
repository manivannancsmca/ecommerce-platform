
ecommerce-platform/
├── pom.xml                          ← Parent POM (dependency management)
├── infrastructure/
│   └── docker-compose.yml           ← All infrastructure containers
├── service-registry/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/registry/
│       │   └── ServiceRegistryApplication.java
│       └── resources/
│           └── application.yml
├── api-gateway/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/gateway/
│       │   ├── ApiGatewayApplication.java
│       │   ├── config/
│       │   │   └── RateLimiterConfig.java
│       │   └── filter/
│       │       ├── CorrelationIdFilter.java
│       │       └── RequestLoggingFilter.java
│       └── resources/
│           └── application.yml
├── user-service/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ecommerce/user/
│       │   └── UserServiceApplication.java
│       └── resources/
│           └── application.yml
├── product-service/                 ← (same skeleton pattern)
├── order-service/                   ← (same skeleton pattern)
├── payment-service/                 ← (same skeleton pattern)
└── ai-service/                      ← (same skeleton pattern)


---

                         ┌──────────────────┐
                         │   Clients / UI    │
                         └────────┬─────────┘
                                  │
                         ┌────────▼─────────┐
                         │   API Gateway     │ :9000
                         │  (Spring Cloud    │
                         │   Gateway)        │
                         └──┬───┬───┬───┬───┘
                            │   │   │   │
              ┌─────────────┘   │   │   └──────────────┐
              │                 │   │                  │
     ┌────────▼──────┐ ┌───────▼───▼──┐ ┌─────────────▼────┐
     │ User Service  │ │Product Svc   │ │ Order Service     │
     │    :8082      │ │   :8083      │ │    :8084          │
     └───────┬───────┘ └──┬───────┬───┘ └──┬───────────────┘
             │            │       │        │        │
     ┌───────▼──────┐ ┌──▼───┐ ┌─▼──┐ ┌───▼──┐ ┌───▼──────┐
     │  MySQL:3306  │ │MySQL │ │ ES │ │Kafka │ │Payment   │
     │  user_db     │ │:3307 │ │    │ │Cluster│ │Service   │
     └──────────────┘ │prod_ │ │    │ │(3 brk)│ │  :8085   │
                      │db    │ └────┘ └───────┘ └───┬──────┘
                      └──────┘                      │
                                              ┌─────▼──────┐
                                              │ MySQL:3309 │
                                              │ payment_db │
                                              └────────────┘

                    ┌───────────────────────────────┐
                    │         Eureka :8761           │
                    │    (Service Registry)          │
                    └───────────────────────────────┘

                    ┌───────────────────────────────┐
                    │  Infrastructure (Docker)       │
                    │  Redis :6379                   │
                    │  Schema Registry :8081         │
                    │  Kafka UI :8090                │
                    │  Jaeger :16686                 │
                    └───────────────────────────────┘

---

## Infrastructure Port Map

 | Container | Host Port | Purpose |
| --- | --- | --- |
| `mysql-user` | `3306` | User Service DB |
| `mysql-product` | `3307` | Product Service DB |
| `mysql-order` | `3308` | Order Service DB |
| `mysql-payment` | `3309` | Payment Service DB |
| `kafka-broker-1` | `9092` | Kafka (host access) |
| `kafka-broker-2` | `9093` | Kafka (host access) |
| `kafka-broker-3` | `9094` | Kafka (host access) |
| `schema-registry` | `8081` | Avro schema management |
| `kafka-ui` | `8090` | Kafka monitoring dashboard |
| `redis` | `6379` | Rate limiter / cache |
| `elasticsearch` | `9200` | Product read model |
| `jaeger` | `16686` | Trace visualization |

---