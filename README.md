# ShopCore

ShopCore is a Java 17 learning monorepo for the OrderFlow backend plan. Each service is an independent Spring Boot application, while Maven and Docker Compose provide one place to build and manage the local environment.

## Modules

| Module | Responsibility | HTTP port | Other port |
|---|---|---:|---:|
| `api-gateway` | Public HTTP routing | 8080 | - |
| `user-service` | Identity context skeleton | 8081 | - |
| `product-service` | Product/inventory context skeleton | 8082 | gRPC 9092 |
| `order-service` | Order context skeleton | 8083 | - |
| `notification-service` | Event-driven notification skeleton | 8084 | - |
| `product-contract` | Versioned Protobuf/gRPC contract | - | - |

The base intentionally contains no CRUD, authentication, cache behavior, event consumers, or order business flow.

## Requirements

- Java 17
- Maven 3.9+ (the Maven Wrapper is preferred)
- Docker with Docker Compose

## Build

```powershell
.\mvnw.cmd clean verify
```

or:

```powershell
.\scripts\build.ps1
```

## Start local infrastructure

Copy `.env.example` to `.env` only when you need to override the development defaults. `.env` is ignored by Git.

```powershell
.\scripts\infra-up.ps1
.\scripts\infra-status.ps1
```

Infrastructure ports:

```text
PostgreSQL user          5433
PostgreSQL product       5434
PostgreSQL order         5435
PostgreSQL notification  5436
Redis                    6379
RabbitMQ                 5672
RabbitMQ Management      15672
```

Stop the infrastructure without deleting data:

```powershell
.\scripts\infra-down.ps1
```

## Run services

Build first, start infrastructure, then run each packaged application in a separate terminal:

```powershell
java -jar api-gateway\target\api-gateway-0.0.1-SNAPSHOT.jar --debug=false
java -jar user-service\target\user-service-0.0.1-SNAPSHOT.jar --debug=false
java -jar product-service\target\product-service-0.0.1-SNAPSHOT.jar --debug=false
java -jar order-service\target\order-service-0.0.1-SNAPSHOT.jar --debug=false
java -jar notification-service\target\notification-service-0.0.1-SNAPSHOT.jar --debug=false
```

From an IDE, run each module's `*Application` main class instead.

Health checks are available at `http://localhost:<service-port>/actuator/health`. RabbitMQ Management is available at `http://localhost:15672`.

## Communication boundaries

```text
Client -> API Gateway                 REST/JSON
API Gateway -> application services  REST/JSON
Order Service -> Product Service     gRPC/Protobuf (contract only in this base)
Order Service -> Notification        RabbitMQ (implementation deferred)
```

Each stateful service owns its PostgreSQL database. Services must not import another service's domain or persistence classes.

## Documentation

- Learning roadmap: `docs/OrderFlow_Java17_Backend_Learning_Plan.md`
- Base implementation status: `docs/BASE_IMPLEMENTATION_PROGRESS.md`
