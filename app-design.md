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