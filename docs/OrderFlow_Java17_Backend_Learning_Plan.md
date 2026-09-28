# OrderFlow Backend Learning Project

> Mục tiêu: xây dựng một project backend đủ nhỏ để tự làm, nhưng đủ sâu để ôn và thực hành **Java Core, Spring Boot, SQL, Redis, RabbitMQ, Multithreading/Concurrency, Microservices, DDD, Hexagonal Architecture, Security, Docker và Testing**.

---

# 1. Mục tiêu của project

Project này không tập trung vào việc làm một hệ thống thương mại điện tử đầy đủ tính năng.

Mục tiêu chính là dùng một business domain quen thuộc để tạo ra các bài toán backend thực tế:

- CRUD và REST API.
- Transaction.
- SQL và query optimization.
- Concurrency và race condition.
- Caching.
- Rate limiting.
- Distributed locking.
- Service-to-service communication.
- Message queue.
- Retry / DLQ / idempotency.
- Authentication / Authorization.
- Domain modeling.
- Clean dependency.
- Distributed transaction.
- Testing.
- Docker deployment.

Sau khi hoàn thành project, cần có khả năng giải thích:

```text
Problem
  ↓
Solution
  ↓
Why this solution?
  ↓
Trade-off
  ↓
Alternative solution
```

Đây là mục tiêu quan trọng hơn việc chỉ "dùng được nhiều công nghệ".

---

# 2. Tech Stack

## Core

```text
Java 17
Spring Boot 4.0.8
Maven
```

## Spring

```text
Spring Web
Spring Data JPA
Spring Validation
Spring Security
Spring Cloud Gateway
Spring AMQP
Spring Cache
gRPC Java
Protocol Buffers
```

## Data

```text
PostgreSQL
Redis
```

> Có thể dùng SQL Server nếu muốn sát với công việc hiện tại, nhưng PostgreSQL được khuyến nghị để mở rộng thêm trải nghiệm DB.

## Messaging

```text
RabbitMQ
```

Kafka chưa cần dùng ở version đầu.

## Testing

```text
JUnit 5
Mockito
Testcontainers
```

## Infrastructure

```text
Docker
Docker Compose
```

## Optional - Phase nâng cao

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
OpenTelemetry
Resilience4j
```

---

# 3. Kiến trúc tổng thể

```text
                         Client
                           |
                        REST/JSON
                           |
                           v
                     API Gateway
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     User Service    Product Service   Order Service
                           ^                |
                           |                |
                           |      gRPC      |
                           +----------------+
                                            |
                                            | Event
                                            v
                                        RabbitMQ
                                            |
                                            v
                                  Notification Service
```

Các service chính:

```text
api-gateway
user-service
product-service
order-service
notification-service
```

Có thể thêm sau:

```text
payment-service
```

---

# 4. Architecture cho từng service

Không bắt buộc tất cả service phải dùng cùng một architecture.

## User Service

Dùng Layered Architecture đơn giản.

```text
Controller
   ↓
Service
   ↓
Repository
   ↓
Database
```

Lý do:

- Business đơn giản.
- Chủ yếu CRUD + authentication.
- Không cần over-engineer.

---

## Product Service

Dùng Hexagonal Architecture.

```text
Adapter In
   ↓
Input Port
   ↓
Application
   ↓
Domain
   ↓
Output Port
   ↓
Adapter Out
```

Ví dụ:

```text
REST Controller
      ↓
InventoryUseCase
      ↓
InventoryService
      ↓
InventoryRepositoryPort
      ↓
JpaInventoryAdapter
```

---

## Order Service

Dùng:

```text
DDD
+
Hexagonal Architecture
```

Đây là service quan trọng nhất để học architecture.

```text
order-service
│
├── domain
│   ├── model
│   ├── event
│   ├── repository
│   └── exception
│
├── application
│   ├── port
│   │   ├── in
│   │   └── out
│   ├── command
│   └── service
│
└── adapter
    ├── in
    │   └── web
    └── out
        ├── persistence
        ├── product
        └── messaging
```

---

## Notification Service

Event-driven đơn giản.

```text
RabbitMQ
   ↓
Consumer
   ↓
Notification Handler
   ↓
Notification Log
```

---

# 5. Bounded Context

Project chia thành các bounded context:

```text
Identity Context
Product Context
Order Context
Notification Context
```

Sau này:

```text
Payment Context
```

Không share Entity giữa các service.

Sai:

```text
order-service
    import ProductEntity from product-service
```

Đúng:

```text
order-service
    ↓
ProductClientPort
    ↓
REST
    ↓
product-service
```

---

# 6. User Service

## Responsibility

```text
Register
Login
Refresh Token
User Profile
Role
Authentication
Authorization
```

## Entity

```text
User
-----------------
id
username
email
password
status
createdAt
updatedAt

Role
-----------------
id
name

RefreshToken
-----------------
id
userId
token
expiredAt
```

## APIs

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout

GET  /api/users/me
PUT  /api/users/me
```

## Knowledge

```text
Spring Security
JWT
Authentication
Authorization
PasswordEncoder
SecurityFilterChain
OncePerRequestFilter
Role / Permission
Refresh Token
```

---

# 7. Product Service

## Responsibility

```text
Product
Category
Inventory
Product Search
Inventory Reservation
```

## Domain

```text
Product
-----------------
id
code
name
description
price
status
categoryId

Category
-----------------
id
name

Inventory
-----------------
id
productId
quantity
version
```

## APIs

```http
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}

GET    /api/products
GET    /api/products/{id}

POST   /api/categories
GET    /api/categories

GET    /api/inventory/{productId}

Internal gRPC:
- GetProduct
- ReserveInventory
- ReleaseInventory
```

---

# 8. Order Service

## Responsibility

```text
Create Order
Get Order
Cancel Order
Complete Order
Order History
```

## Aggregate

```text
Order
 |
 +-- OrderItem
 +-- OrderItem
 +-- OrderItem
```

`Order` là Aggregate Root.

Không cho external code sửa trực tiếp `OrderItem`.

Sai:

```java
order.getItems().get(0).setQuantity(100);
```

Đúng:

```java
order.changeItemQuantity(productId, 100);
```

## Domain Model

```text
Order
-----------------
id
userId
status
totalAmount
createdAt

OrderItem
-----------------
id
productId
productName
quantity
unitPrice
```

## Value Object

```text
OrderId
Money
Quantity
```

Ví dụ:

```java
public record Money(BigDecimal value) {

    public Money {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Money must be >= 0");
        }
    }
}
```

## Order Status

```text
PENDING
CONFIRMED
CANCELLED
COMPLETED
```

## Business Rule

Ví dụ:

```text
Order phải có ít nhất 1 item.

Quantity phải > 0.

Order COMPLETED không được cancel.

Order CANCELLED không được complete.

TotalAmount không được âm.
```

---

# 9. APIs của Order Service

```http
POST /api/orders

GET /api/orders/{id}

GET /api/orders

GET /api/orders/my-orders

POST /api/orders/{id}/cancel

POST /api/orders/{id}/complete
```

---

# 10. Flow tạo Order

```text
Client
   |
   v
API Gateway
   |
   v
Order Service
   |
   | gRPC
   v
Product Service
   |
   | Validate Product
   | Reserve Stock
   |
   v
Order Service
   |
   | Save Order
   |
   v
Order DB
   |
   | Publish Event
   v
RabbitMQ
   |
   v
Notification Service
```

Request:

```json
{
  "items": [
    {
      "productId": "uuid-1",
      "quantity": 2
    },
    {
      "productId": "uuid-2",
      "quantity": 1
    }
  ]
}
```

---

# 11. Multithreading & Concurrency Lab

Đây là một phần bắt buộc của project.

---

## 11.1 Java Thread Basics

Học:

```text
Thread
Runnable
Callable
Future
```

Bài tập:

```text
Tạo 10 task.

Mỗi task sleep random 100-500ms.

Chạy sequential.

Chạy bằng Thread.

So sánh thời gian.
```

---

## 11.2 ExecutorService

Học:

```text
FixedThreadPool
CachedThreadPool
ScheduledThreadPool
shutdown
awaitTermination
```

Ví dụ:

```java
ExecutorService executor =
    Executors.newFixedThreadPool(10);
```

Bài tập:

```text
100 task
10 worker thread

Quan sát:
- thread reuse
- queue
- execution order
```

---

## 11.3 Callable + Future

Bài tập:

```text
Task trả về giá product.

Main thread chờ kết quả.
```

Học:

```text
Future.get()
timeout
exception
cancel
```

---

# 12. CompletableFuture

Bài toán:

Order Service cần:

```text
Product Info
User Info
```

Nếu gọi tuần tự:

```text
Product API = 300 ms
User API    = 200 ms

Total ≈ 500 ms
```

Nếu gọi song song:

```text
Total ≈ max(300, 200)

≈ 300 ms
```

Thực hành:

```text
CompletableFuture
supplyAsync
thenApply
thenCompose
thenCombine
allOf
exceptionally
handle
orTimeout
```

Không chỉ học happy path.

Phải xử lý:

```text
Product service timeout
User service exception
Một future fail
Executor bị đầy
```

---

# 13. Race Condition Lab

Đây là bài quan trọng nhất.

Initial:

```text
Stock = 10
```

Gửi:

```text
100 purchase requests
```

Expected:

```text
10 success
90 failed
stock = 0
```

Code ban đầu cố tình unsafe:

```java
Inventory inventory =
    repository.findByProductId(productId);

if (inventory.getQuantity() >= quantity) {
    inventory.setQuantity(
        inventory.getQuantity() - quantity
    );

    repository.save(inventory);
}
```

Chạy concurrent test và quan sát Lost Update.

---

# 14. CountDownLatch

Dùng để cho 100 thread bắt đầu gần cùng lúc.

Ví dụ:

```java
CountDownLatch ready =
    new CountDownLatch(100);

CountDownLatch start =
    new CountDownLatch(1);

CountDownLatch done =
    new CountDownLatch(100);
```

Mục tiêu:

```text
100 threads ready
        ↓
start.countDown()
        ↓
100 requests bắt đầu đồng thời
```

---

# 15. synchronized

Thử fix race condition:

```java
public synchronized void purchase(...) {
}
```

Sau đó hiểu hạn chế:

```text
Instance A
   synchronized

Instance B
   synchronized
```

Không có tác dụng lock giữa 2 JVM khác nhau.

Kết luận:

```text
synchronized
```

chỉ giải quyết concurrency trong cùng JVM/process.

---

# 16. Lock API

Học:

```text
ReentrantLock
tryLock
lockInterruptibly
ReadWriteLock
```

Ví dụ:

```java
Lock lock = new ReentrantLock();

lock.lock();

try {
    // critical section
} finally {
    lock.unlock();
}
```

So sánh với:

```text
synchronized
```

---

# 17. Atomic

Học:

```text
AtomicInteger
AtomicLong
AtomicReference
CAS
```

Bài tập:

```text
100 thread tăng counter.

So sánh:

int
synchronized
AtomicInteger
```

---

# 18. Concurrent Collections

Thực hành:

```text
ConcurrentHashMap
CopyOnWriteArrayList
BlockingQueue
```

Hiểu tại sao:

```text
HashMap
```

không an toàn khi nhiều thread cùng modify.

---

# 19. Database Concurrency

Sau Java concurrency chuyển sang DB concurrency.

Đây là phần quan trọng cho backend thực tế.

---

## 19.1 Optimistic Lock

Entity:

```java
@Version
private Long version;
```

Flow:

```text
Thread A read version = 1
Thread B read version = 1

A update -> version = 2

B update WHERE version = 1

=> fail
```

Học:

```text
OptimisticLockException
Retry
When optimistic locking works well
```

---

# 20. Pessimistic Lock

Ví dụ:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Inventory> findByProductId(UUID productId);
```

Database:

```sql
SELECT ...
FOR UPDATE;
```

Hiểu:

```text
Blocking
Deadlock
Lock timeout
Performance trade-off
```

---

# 21. Atomic SQL Update

Implement:

```sql
UPDATE inventory
SET quantity = quantity - :quantity
WHERE product_id = :productId
  AND quantity >= :quantity;
```

Nếu:

```text
affectedRows = 1
```

=> success.

Nếu:

```text
affectedRows = 0
```

=> out of stock.

So sánh:

```text
Read -> Check -> Write

vs

Atomic UPDATE
```

Đây nên là solution chính cho inventory.

---

# 22. Transaction Isolation

Học:

```text
READ COMMITTED
REPEATABLE READ
SERIALIZABLE
```

Các anomaly:

```text
Dirty Read
Non-repeatable Read
Phantom Read
Lost Update
```

Viết test để quan sát nếu có thể.

---

# 23. Deadlock Lab

Tạo intentionally:

```text
Transaction A
lock Product 1
lock Product 2

Transaction B
lock Product 2
lock Product 1
```

Quan sát deadlock.

Sau đó fix bằng:

```text
Always lock resources in same order
```

---

# 24. Redis

Redis có 3 use case chính.

---

## 24.1 Product Cache

Cache key:

```text
product:{id}
```

Flow:

```text
Request
   ↓
Redis
   |
 HIT
   ↓
Return

MISS
   ↓
Database
   ↓
Redis
   ↓
Return
```

Học:

```text
Cache Aside
TTL
Cache Miss
Cache Invalidation
```

---

# 25. Cache Problems

Tạo bài test cho:

```text
Cache Penetration
Cache Stampede
Cache Avalanche
```

Ví dụ Cache Stampede:

```text
100 requests
     ↓
cache hết hạn
     ↓
100 requests cùng query DB
```

Thử giải pháp:

```text
Distributed Lock
Random TTL
Logical Expiration
```

---

# 26. Redis Rate Limit

Endpoint:

```http
POST /api/orders
```

Rule:

```text
10 request / minute / user
```

Version 1:

```text
INCR + EXPIRE
```

Version 2:

```text
Sliding Window
```

Dùng:

```text
Redis ZSET
```

Key:

```text
rate-limit:order:{userId}
```

---

# 27. Redis Distributed Lock

Key:

```text
lock:inventory:{productId}
```

Có thể dùng:

```text
Redisson
```

Thực hành:

```java
RLock lock =
    redissonClient.getLock(key);
```

Quan trọng:

Không dùng Redis Lock ngay từ đầu.

Thứ tự học:

```text
Java lock
    ↓
DB lock
    ↓
Atomic SQL
    ↓
Redis Distributed Lock
```

Để hiểu tại sao distributed lock tồn tại.

---

# 27.5. gRPC Lab

Ngoài RPC thật giữa Order Service và Product Service, tạo một lab nhỏ để học đủ 4 kiểu RPC.

## Unary

```text
1 request
1 response
```

Dùng thật trong business:

```text
GetProduct
ReserveInventory
ReleaseInventory
```

## Server Streaming

Lab:

```text
WatchInventory
```

Client gửi `productId`, server trả nhiều inventory update.

## Client Streaming

Lab:

```text
BulkInventoryAdjustment
```

Client gửi nhiều adjustment, server trả kết quả tổng.

## Bidirectional Streaming

Lab đơn giản:

```text
InventorySync
```

Không bắt buộc đưa vào production flow.

## Checklist

Phải tự trả lời được:

```text
gRPC dùng HTTP/2 để làm gì?

Protobuf khác JSON thế nào?

Blocking Stub vs Async Stub?

Deadline dùng để làm gì?

StatusRuntimeException xử lý thế nào?

Metadata dùng làm gì?

Interceptor tương tự filter ở điểm nào?

Có nên expose gRPC trực tiếp cho frontend không?

Khi nào REST phù hợp hơn gRPC?

Khi nào RabbitMQ phù hợp hơn gRPC?
```

---

# 28. RabbitMQ

Topology ban đầu:

```text
order.exchange
```

Routing keys:

```text
order.created
order.cancelled
```

Queues:

```text
notification.order.created.queue

notification.order.cancelled.queue
```

Flow:

```text
Order Service
     |
     | OrderCreatedEvent
     v
order.exchange
     |
     v
notification.order.created.queue
     |
     v
Notification Service
```

---

# 29. Event Model

Ví dụ:

```java
public record OrderCreatedEvent(
    UUID eventId,
    UUID orderId,
    UUID userId,
    BigDecimal totalAmount,
    Instant createdAt
) {}
```

`eventId` dùng để xử lý idempotency.

---

# 30. RabbitMQ Concepts cần học

```text
Producer
Consumer
Queue
Exchange
Routing Key

Direct Exchange
Topic Exchange
Fanout Exchange

ACK
NACK
Requeue

Durable Queue
Persistent Message

Prefetch
Concurrency

Retry
DLQ
DLX
```

---

# 31. Retry

Giả lập consumer fail.

```text
Message
   ↓
Consumer
   ↓
FAIL
   ↓
Retry #1
   ↓
Retry #2
   ↓
Retry #3
   ↓
DLQ
```

Không được retry vô hạn.

---

# 32. Dead Letter Queue

Ví dụ:

```text
notification.order.created.dlq
```

Lưu message không xử lý được để:

```text
inspect
debug
retry manually
```

---

# 33. Idempotent Consumer

RabbitMQ có thể deliver message nhiều hơn một lần.

Notification DB:

```text
processed_event
-----------------
event_id
processed_at
```

Flow:

```text
Receive event
     ↓
eventId exists?
   /        \
 YES        NO
  |          |
 skip      process
             |
             v
         save eventId
```

Học:

```text
At-least-once delivery
Duplicate message
Idempotency
```

---

# 34. Notification Service

Responsibility:

```text
Order Notification
Mock Email
Mock Push Notification
```

API không cần nhiều.

Chủ yếu:

```text
RabbitMQ Consumer
```

DB:

```text
notification_log
-----------------
id
eventId
userId
type
status
message
createdAt
```

---

# 35. API Gateway

Dùng:

```text
Spring Cloud Gateway
```

Route:

```text
/api/auth/**
      ↓
user-service

/api/users/**
      ↓
user-service

/api/products/**
      ↓
product-service

/api/orders/**
      ↓
order-service
```

Gateway chạy:

```text
localhost:8080
```

Services:

```text
user-service        8081
product-service     8082
order-service       8083
notification-service 8084
```

---

# 36. Service-to-Service Communication

Internal synchronous communication dùng:

```text
gRPC + Protocol Buffers
```

Flow chính:

```text
Order Service
     |
     | gRPC
     v
Product Service
```

Các RPC ban đầu:

```proto
service ProductService {
  rpc GetProduct(GetProductRequest)
      returns (GetProductResponse);

  rpc ReserveInventory(ReserveInventoryRequest)
      returns (ReserveInventoryResponse);

  rpc ReleaseInventory(ReleaseInventoryRequest)
      returns (ReleaseInventoryResponse);
}
```

Order Service không phụ thuộc trực tiếp vào generated stub trong application layer.

Hexagonal flow:

```text
CreateOrderUseCase
       |
       v
InventoryPort
       |
       v
GrpcInventoryAdapter
       |
       v
ProductServiceBlockingStub
       |
       v
Product Service
```

Các concept phải học:

```text
.proto
Protocol Buffers
Generated Java Stub

ManagedChannel
Blocking Stub
Async Stub

Unary RPC
Server Streaming
Client Streaming
Bidirectional Streaming

Deadline
Timeout
Metadata
Interceptor
Status Code

Schema Evolution
Backward Compatibility
```

Version đầu chỉ dùng Unary RPC trong business flow.

Các loại streaming được làm thành lab riêng, không cố nhét vào business nếu không cần.

### gRPC error handling

Phải xử lý các status thường gặp:

```text
INVALID_ARGUMENT
NOT_FOUND
RESOURCE_EXHAUSTED
FAILED_PRECONDITION
UNAVAILABLE
DEADLINE_EXCEEDED
INTERNAL
```

Ví dụ Order Service phải đặt deadline khi gọi Product Service.

Không để RPC chờ vô hạn.

### REST vs gRPC vs RabbitMQ

```text
Client -> Gateway / Public API
REST + JSON

Order -> Product
gRPC + Protobuf

Order -> Notification
RabbitMQ
```

Mục tiêu là hiểu được trade-off giữa:

```text
REST
gRPC
Message Queue
```

---

# 37. Resilience

Sau khi REST communication chạy ổn, thêm:

```text
Resilience4j
```

Học:

```text
Retry
Circuit Breaker
TimeLimiter
Bulkhead
```

Case:

```text
Product Service down
```

Order Service phải xử lý ra sao?

---

# 38. SQL Learning Plan

Không chỉ dùng JPA-generated query.

Phải tự viết SQL.

---

## Query Exercises

### Order History

```text
Danh sách order của user.
```

### Revenue

```text
Revenue theo ngày.
```

### Top Product

```text
Top 10 sản phẩm bán chạy.
```

### Product Search

```text
Search theo name + category.
```

### Low Stock

```text
Product có stock < 10.
```

### Order Report

```text
Count order theo status.
```

---

# 39. SQL Topics phải thực hành

```text
JOIN
LEFT JOIN
GROUP BY
HAVING
Subquery
EXISTS
NOT EXISTS
CTE
Window Function

Pagination

Index
Composite Index
Covering Index

Execution Plan

Transaction
Lock
Isolation Level
Deadlock
```

---

# 40. SQL Optimization Lab

Generate data:

```text
1,000,000 Order

3,000,000 OrderItem

100,000 Product
```

Không nhất thiết tạo từ đầu project.

Dùng ở phase optimization.

Thực hiện:

```text
Query chậm
   ↓
Execution Plan
   ↓
Identify Scan
   ↓
Create Index
   ↓
Measure again
```

Luôn ghi lại:

```text
Before

Execution Time
Logical Reads
Execution Plan
```

và:

```text
After
```

---

# 41. Java Core trong Project

Không học Java Core riêng hoàn toàn.

Dùng ngay trong project.

---

## OOP

```text
Encapsulation
Abstraction
Inheritance
Polymorphism
```

---

## Collections

```text
List
Set
Map
Queue
```

Ví dụ:

```text
List<OrderItem>
Set<Role>
Map<UUID, ProductInfo>
```

---

## Stream API

Thực hành:

```text
map
filter
flatMap
groupingBy
toMap
reduce
sorted
distinct
```

Không abuse Stream cho logic khó đọc.

---

## Generic

Ví dụ:

```java
public record ApiResponse<T>(
    T data,
    String message
) {}
```

---

## Optional

Dùng đúng:

```java
repository.findById(id)
    .orElseThrow(...);
```

Không dùng Optional:

```text
field
DTO property
method parameter
```

trừ khi có lý do rõ ràng.

---

## Exception

```text
Domain Exception
Application Exception
Infrastructure Exception
```

Ví dụ:

```text
ProductNotFoundException
OutOfStockException
InvalidOrderStateException
OrderNotFoundException
```

---

# 42. Java 17

Các feature nên dùng trong project:

```text
record
sealed class / sealed interface
switch expression
pattern matching for instanceof
text blocks
Stream API
Optional
CompletableFuture
```

Ví dụ `record` rất hợp cho:

```text
Request DTO
Response DTO
Command
Event payload
Value Object đơn giản
```

Ví dụ:

```java
public record CreateOrderCommand(
    UUID userId,
    List<CreateOrderItemCommand> items
) {}
```

Lưu ý:

```text
Java 17 chưa có Virtual Threads chính thức.
Không đưa Virtual Thread vào core project.
```

Nếu sau này muốn so sánh Java 17 với Java 21 thì tạo branch/lab riêng.

---

# 43. Java 17 Concurrency Lab

Với Java 17, tập trung vào:

```text
Thread
Runnable
Callable
ExecutorService
ThreadPoolExecutor
Future
CompletableFuture
synchronized
volatile
Lock / ReentrantLock
Atomic classes
CountDownLatch
ConcurrentHashMap
BlockingQueue
```

Các bài chính:

```text
Sequential vs Thread Pool
Runnable vs Callable
Future timeout
CompletableFuture parallel calls
Race Condition
Atomic Counter
Producer / Consumer với BlockingQueue
Inventory concurrent update
```

Phải hiểu:

```text
Thread creation cost
Thread pool sizing
CPU-bound vs I/O-bound
Task queue
Thread starvation
Deadlock
Visibility
Atomicity
Happens-before
```

---

# 44. Spring Transaction

Các bài phải thử:

```text
@Transactional

RuntimeException rollback

CheckedException behavior

Propagation

Isolation

readOnly

self-invocation problem
```

Đặc biệt:

```text
Method A @Transactional

Method A gọi Method B @Transactional
trong cùng class
```

Phải hiểu Spring Proxy.

---

# 45. Spring Bean

Ôn:

```text
Bean Lifecycle
@Component
@Service
@Repository
@Configuration
@Bean

Singleton
Prototype
```

Thử:

```text
@PostConstruct
@PreDestroy
```

---

# 46. Spring Security

Flow:

```text
Request
   ↓
Security Filter
   ↓
JWT Validation
   ↓
Authentication
   ↓
Authorization
   ↓
Controller
```

Roles:

```text
USER
ADMIN
```

Rule:

```text
USER
- create order
- view own orders

ADMIN
- CRUD product
- view all orders
```

---

# 47. Docker

Docker Compose gồm:

```text
api-gateway
user-service
product-service
order-service
notification-service

postgres-user
postgres-product
postgres-order
postgres-notification

redis
rabbitmq
```

Sau này có thể thêm:

```text
prometheus
grafana
```

---

# 48. Testing Strategy

## Unit Test

Test:

```text
Domain
Use Case
Business Rule
```

Ví dụ:

```text
cancelCompletedOrder_shouldFail

createOrder_emptyItems_shouldFail

money_negative_shouldFail
```

---

## Repository Integration Test

Dùng:

```text
Testcontainers PostgreSQL
```

Test:

```text
query
transaction
lock
index behavior
```

---

## Redis Integration Test

Dùng:

```text
Testcontainers Redis
```

Test:

```text
Cache
Rate Limit
Lock
```

---

## RabbitMQ Integration Test

Dùng:

```text
Testcontainers RabbitMQ
```

Test:

```text
publish
consume
retry
DLQ
idempotency
```

---

# 49. Concurrency Test

Test quan trọng:

```text
Initial Stock = 10

Concurrent Requests = 100
```

Expected:

```text
Success = 10
Fail = 90
Final Stock = 0
```

Chạy test với các implementation:

```text
Unsafe
synchronized
Optimistic Lock
Pessimistic Lock
Atomic SQL
Redis Lock
```

Ghi lại:

```text
Correctness
Throughput
Latency
Complexity
```

---

# 50. Logging

Log format nên có:

```text
timestamp
level
service
traceId
requestId
userId
orderId
message
```

Ví dụ:

```text
2026-09-15
INFO
order-service
traceId=abc123
orderId=xxx
Order created
```

---

# 51. Observability

Sau khi core hoàn thành:

```text
Spring Boot Actuator
Micrometer
Prometheus
Grafana
```

Metrics:

```text
request_count
request_latency
order_created
order_failed
inventory_reserve_failed
rabbit_consumer_failed
```

---

# 52. Transactional Outbox

Đây là phase nâng cao rất đáng học.

Problem:

```text
BEGIN

Save Order

COMMIT

Publish RabbitMQ
      ↓
     FAIL
```

Kết quả:

```text
Order tồn tại
Event bị mất
```

Solution:

```text
BEGIN

Insert Order
Insert Outbox Event

COMMIT
```

Worker:

```text
Outbox
   ↓
Publisher
   ↓
RabbitMQ
```

---

# 53. Outbox Table

```text
outbox_event
----------------------
id
aggregateId
aggregateType
eventType
payload
status
createdAt
publishedAt
```

Status:

```text
NEW
PUBLISHED
FAILED
```

---

# 54. Payment Service - Optional

Sau core project mới thêm.

Flow:

```text
Order
   ↓
Reserve Inventory
   ↓
Payment
   ↓
Confirm Order
```

Payment mock.

Không tích hợp payment thật.

---

# 55. Saga

Khi thêm Payment Service:

```text
Create Order
   ↓
Reserve Inventory
   ↓
Create Payment
```

Nếu payment fail:

```text
Payment Failed
     ↓
Release Inventory
     ↓
Cancel Order
```

Học:

```text
Compensating Transaction
Saga Choreography
Saga Orchestration
```

Bắt đầu bằng choreography.

---

# 56. Không làm ngay

Version đầu KHÔNG cần:

```text
Kafka
Kubernetes
Eureka
Event Sourcing
CQRS full
ElasticSearch
Service Mesh
GraphQL
```

Chỉ thêm nếu core system đã hoàn chỉnh.

---

# 57. Repository Structure

Có thể dùng mono-repo:

```text
order-flow/
│
├── api-gateway/
│
├── user-service/
│
├── product-service/
│
├── order-service/
│
├── notification-service/
│
├── docker/
│
├── scripts/
│
├── docs/
│
└── docker-compose.yml
```

---

# 58. Git Strategy

Branches:

```text
main

develop

feature/product-crud
feature/order-domain
feature/inventory-concurrency
feature/redis-cache
feature/rabbitmq
```

Commit rõ mục đích.

Ví dụ:

```text
feat: implement inventory atomic update

fix: prevent duplicate rabbitmq event processing

test: add concurrent inventory reservation test

perf: add composite index for order history
```

---

# 59. Roadmap hoàn chỉnh

## Phase 0 - Setup

Mục tiêu:

```text
Create repository
Docker Compose
PostgreSQL
Redis
RabbitMQ
```

Deliverables:

```text
docker compose up

All infrastructure running
```

---

# Phase 1 - Java Core Refresher

Học song song khi code.

Topics:

```text
OOP
Collections
Stream
Generics
Exception
Optional
Date Time
BigDecimal
Enum
Record
```

Bài tập nhỏ viết trong:

```text
java-lab/
```

hoặc test package.

---

# Phase 2 - Product Service

Implement:

```text
Product CRUD
Category CRUD
Inventory
Validation
Exception Handling
Pagination
```

Architecture:

```text
Hexagonal
```

Knowledge:

```text
Spring Boot
REST
JPA
Validation
SQL
Hexagonal
```

---

# Phase 3 - Order Domain

Implement:

```text
Order
OrderItem
OrderStatus
Money
OrderId
Business Rules
```

Không cần database ngay ở bước đầu.

Test domain trước.

Knowledge:

```text
DDD Entity
Value Object
Aggregate
Invariant
Domain Exception
```

---

# Phase 4 - Order Persistence

Implement:

```text
Order Repository Port
JPA Adapter
Mapping Domain <-> JPA
```

Knowledge:

```text
Hexagonal
Dependency Inversion
Persistence Mapping
Transaction
```

---

# Phase 5 - Service-to-Service gRPC

Implement:

```text
Order Service
     ↓ gRPC
Product Service
```

RPCs:

```text
GetProduct
ReserveInventory
ReleaseInventory
```

Knowledge:

```text
Protocol Buffers
.proto
Generated Stub
ManagedChannel
Blocking Stub
Deadline
Status Code
Error Mapping
gRPC Interceptor
Schema Evolution
```

---

# Phase 6 - Java Multithreading Basics

Implement labs:

```text
Thread
Runnable
Callable
ExecutorService
Future
CompletableFuture
CountDownLatch
AtomicInteger
ConcurrentHashMap
```

Không merge các lab này với production logic nếu không cần.

---

# Phase 7 - Inventory Concurrency

Implement lần lượt:

```text
Unsafe Version

synchronized

ReentrantLock

Optimistic Lock

Pessimistic Lock

Atomic SQL Update
```

Test:

```text
100 request
10 stock
```

Viết report so sánh.

---

# Phase 8 - Database Deep Dive

Topics:

```text
Transaction
Isolation
Lock
Deadlock
Index
Execution Plan
```

Generate large data.

Optimize queries.

---

# Phase 9 - Redis Cache

Implement:

```text
Product Cache
TTL
Cache Invalidation
```

Test cache hit / miss.

---

# Phase 10 - Redis Advanced

Implement:

```text
Rate Limiting
Distributed Lock
```

Compare Redis Lock với DB solution.

---

# Phase 11 - RabbitMQ

Implement:

```text
OrderCreatedEvent
OrderCancelledEvent

Producer
Exchange
Queue
Consumer
```

---

# Phase 12 - RabbitMQ Reliability

Implement:

```text
Manual ACK

Retry

DLQ

Idempotent Consumer
```

Test duplicate message.

---

# Phase 13 - Notification Service

Implement:

```text
Notification Consumer
Notification Log
Mock Email
```

---

# Phase 14 - User Service

Implement:

```text
Register
Login
JWT
Refresh Token
Role
Authorization
```

---

# Phase 15 - API Gateway

Implement:

```text
Routing
Authentication forwarding
Request logging
```

---

# Phase 16 - Dockerize

Dockerize:

```text
all Spring services
```

Goal:

```text
docker compose up
```

khởi động được toàn bộ system.

---

# Phase 17 - Testing

Add:

```text
Unit Tests

Integration Tests

Testcontainers

Concurrency Tests
```

---

# Phase 18 - Resilience

Add:

```text
Timeout
Retry
Circuit Breaker
```

Dùng Resilience4j.

---

# Phase 19 - Outbox

Implement:

```text
Transactional Outbox
Publisher Worker
Retry
Status
```

---

# Phase 20 - Observability

Implement:

```text
Actuator
Metrics
Prometheus
Grafana
Trace ID
```

---

# Phase 21 - Payment + Saga

Optional.

Implement:

```text
Payment Service

Saga

Compensation
```

---

# 60. Suggested Learning Order

```text
Java Core
   ↓
Spring Boot
   ↓
gRPC / REST
   ↓
Hexagonal
   ↓
DDD
   ↓
SQL
   ↓
Transaction
   ↓
Java Concurrency
   ↓
Database Concurrency
   ↓
Redis
   ↓
RabbitMQ
   ↓
Security
   ↓
Microservice Reliability
   ↓
Docker
   ↓
Testing
   ↓
Outbox
   ↓
Saga
```

---

# 61. Definition of Done cho mỗi feature

Một feature chỉ được xem là hoàn thành khi:

```text
Business logic works

Validation exists

Error handling exists

Unit test exists

Integration test nếu cần

Logging đủ dùng

Không phá architecture boundary

README/update docs

Happy path + failure path đều test
```

---

# 62. Questions phải trả lời được sau project

## Java Core

```text
Thread vs Runnable?

Runnable vs Callable?

Future vs CompletableFuture?

synchronized hoạt động thế nào?

volatile giải quyết gì?

AtomicInteger hoạt động dựa trên gì?

Race condition là gì?

Deadlock là gì?

Thread pool dùng để làm gì?
```

---

## Spring

```text
Bean lifecycle?

DI?

@Component vs @Bean?

@Repository vs @Service?

@Transactional hoạt động thế nào?

Self invocation @Transactional?

Spring Security flow?
```

---

## SQL

```text
Index hoạt động thế nào?

Composite index order?

Clustered vs nonclustered?

Execution Plan đọc như nào?

Optimistic vs Pessimistic Lock?

Isolation Level?

Deadlock?
```

---

## Redis

```text
Cache Aside?

TTL?

Cache Stampede?

Rate Limit?

Redis Lock?

Redis Lock có thay DB Lock không?
```

---

## RabbitMQ

```text
Exchange?

Queue?

Routing key?

ACK/NACK?

Retry?

DLQ?

Prefetch?

Duplicate message?

Idempotent consumer?
```

---

## Architecture

```text
Layered vs Hexagonal?

DDD là gì?

Entity vs Value Object?

Aggregate Root?

Bounded Context?

Tại sao không share DB giữa microservice?

REST vs gRPC?

gRPC vs RabbitMQ?

Khi nào dùng Unary RPC và khi nào dùng Streaming?

Deadline trong gRPC để làm gì?

Protocol Buffers schema evolution cần lưu ý gì?

Microservice có cần Kafka không?

Distributed transaction xử lý sao?
```

---

# 63. Các milestone chính

## Milestone 1

```text
Product + Order chạy bằng REST.
```

## Milestone 2

```text
Concurrency inventory đúng.
```

## Milestone 3

```text
Redis hoạt động.
```

## Milestone 4

```text
RabbitMQ + Notification hoạt động.
```

## Milestone 5

```text
JWT + Gateway hoạt động.
```

## Milestone 6

```text
Docker Compose chạy full system.
```

## Milestone 7

```text
Outbox + reliability.
```

## Milestone 8

```text
Saga / Payment optional.
```

---

# 64. Version 1 MVP

MVP chỉ cần:

```text
API Gateway

Product Service
Order Service
Notification Service

PostgreSQL
Redis
RabbitMQ

Product CRUD

Inventory

Create Order

Cancel Order

Inventory concurrency

Product cache

gRPC Order -> Product

OrderCreatedEvent

Notification consumer

Retry + DLQ

Docker Compose
```

Chưa cần:

```text
Payment

Saga

Outbox

Monitoring

Service Discovery
```

---

# 65. Version 2

Sau MVP:

```text
User Service

JWT

Rate Limit

Redis Distributed Lock

Testcontainers

Resilience4j

Advanced SQL
```

---

# 66. Version 3

Advanced:

```text
Transactional Outbox

Payment Service

Saga

Observability

Performance Test
```

---

# 67. Nguyên tắc quan trọng khi làm project

## 1. Không thêm công nghệ nếu chưa có problem

Không:

```text
"Thêm Redis vì CV cần Redis."
```

Mà:

```text
"Product read nhiều, DB bị query lặp lại
=> cần cache
=> Redis."
```

---

## 2. Luôn làm phiên bản đơn giản trước

Ví dụ concurrency:

```text
Unsafe
  ↓
synchronized
  ↓
DB Lock
  ↓
Atomic SQL
  ↓
Redis Lock
```

---

## 3. Benchmark trước và sau

Ví dụ SQL:

```text
Before index
After index
```

Redis:

```text
DB only
Cache enabled
```

Concurrency:

```text
Optimistic
Pessimistic
Atomic SQL
```

---

## 4. Ghi lại lesson learned

Trong `/docs`:

```text
CONCURRENCY.md

REDIS.md

RABBITMQ.md

SQL_OPTIMIZATION.md

DDD.md

HEXAGONAL.md
```

Mỗi file ghi:

```text
Problem

Root Cause

Solution

Alternative

Trade-off
```

---

# 68. Kết quả cuối cùng mong muốn

Sau project này, không chỉ nói:

```text
Em biết Spring Boot.

Em biết Redis.

Em biết RabbitMQ.

Em biết Microservice.
```

Mà có thể nói:

```text
Trong project MiniCommerce,
em xử lý race condition ở inventory khi nhiều request mua đồng thời.

Ban đầu em thử synchronized nhưng nhận ra nó chỉ đảm bảo trong một JVM.

Sau đó em thử optimistic lock, pessimistic lock và atomic SQL update.

Cuối cùng em chọn atomic update cho inventory vì đơn giản,
đảm bảo atomicity ở DB và giảm lock contention.
```

Hoặc:

```text
Em dùng Redis cho product cache và rate limiting.

RabbitMQ dùng cho asynchronous notification.

Em thiết kế consumer idempotent vì RabbitMQ sử dụng
at-least-once delivery nên message có thể được xử lý nhiều lần.
```

Đó là mục tiêu thực sự của project.

---

# 69. Final Architecture

```text
                               Client
                                 |
                                 v
                           API Gateway
                                 |
              +------------------+------------------+
              |                  |                  |
              v                  v                  v
         User Service      Product Service      Order Service
              |                  |                  |
              |                  |                  |
          PostgreSQL         PostgreSQL         PostgreSQL
                                 |
                               Redis
                                                    |
                                                    |
                                              Order Events
                                                    |
                                                    v
                                                RabbitMQ
                                                    |
                                                    v
                                          Notification Service
                                                    |
                                                PostgreSQL
```

Advanced version:

```text
                              Order Service
                                   |
                 +-----------------+----------------+
                 |                                  |
                 v                                  v
          Product Service                     Payment Service
                 |                                  |
                 +---------------+------------------+
                                 |
                                Saga
                                 |
                                 v
                              RabbitMQ
```

---

# 70. Project Goal Summary

Project tập trung vào:

```text
Java Core
Spring Boot
Spring Security

SQL
Transaction
Index
Execution Plan

Multithreading
Concurrency
DB Locking
Distributed Lock

Redis

RabbitMQ

Microservices

gRPC
Protocol Buffers

DDD

Hexagonal Architecture

REST
gRPC / Protocol Buffers

Docker

Testing

Distributed System Reliability
```

Không cố làm project lớn.

Mục tiêu:

> Một hệ thống nhỏ nhưng mỗi feature đều có lý do kỹ thuật rõ ràng và có thể dùng để giải thích trong phỏng vấn Java Backend.
