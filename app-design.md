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
