# Phân tích nghiệp vụ User Service và API Gateway

## 1. Mục đích tài liệu

Tài liệu này mô tả nghiệp vụ và ranh giới trách nhiệm giữa `user-service`, `api-gateway` và các service nghiệp vụ khác trong ShopCore. Trọng tâm là:

- Đăng ký, đăng nhập và quản lý hồ sơ người dùng.
- Phát hành, sử dụng, làm mới và thu hồi token.
- Xác định nơi thực hiện authentication và authorization.
- Truyền danh tính người dùng từ Gateway tới Product Service và Order Service.
- Xác định các rule bảo mật, dữ liệu, API và tiêu chí nghiệm thu.

Tài liệu là thiết kế mục tiêu cho giai đoạn triển khai authentication. Nó không khẳng định các chức năng bên dưới đã được code.

## 2. Hiện trạng source code

### User Service

`user-service` hiện có:

- Spring MVC.
- Bean Validation.
- Spring Data JPA.
- PostgreSQL cho runtime và H2 cho test.
- Actuator health endpoint.
- Package boundary: `controller`, `service`, `repository`, `model`.

`user-service` hiện chưa có:

- Entity và migration cho user, role, refresh token.
- API đăng ký, đăng nhập, refresh, logout và profile.
- Spring Security.
- JWT encoder/decoder.
- Password encoder.
- Authentication và authorization implementation.

### API Gateway

`api-gateway` hiện dùng Spring Cloud Gateway WebFlux và đang route:

| Route | Path | Destination |
|---|---|---|
| `user-auth` | `/api/auth/**` | User Service |
| `user-profile` | `/api/users/**` | User Service |
| `product-products` | `/api/products/**` | Product Service |
| `product-categories` | `/api/categories/**` | Product Service |
| `product-inventory` | `/api/inventory/**` | Product Service |
| `order` | `/api/orders/**` | Order Service |

Gateway hiện mới thực hiện routing và health check, chưa validate JWT hoặc áp dụng authorization rule.

## 3. Bounded context và trách nhiệm

### 3.1. User Service — Identity Context

User Service là nguồn dữ liệu chuẩn cho:

- Tài khoản người dùng.
- Credential đăng nhập.
- Password hash.
- Trạng thái tài khoản.
- Role của người dùng.
- Refresh token và trạng thái thu hồi.
- Hồ sơ cá nhân cơ bản.
- Phát hành access token và refresh token.

User Service không sở hữu:

- Sản phẩm và tồn kho.
- Đơn hàng.
- Quy tắc một user được sửa sản phẩm hay được xem đơn hàng cụ thể nào.
- Dữ liệu domain của Product Service hoặc Order Service.

### 3.2. API Gateway — Edge Boundary

API Gateway chịu trách nhiệm:

- Là entry point công khai duy nhất cho frontend/client.
- Route request tới đúng service.
- Validate access token tại biên hệ thống.
- Từ chối sớm token thiếu, sai hoặc hết hạn.
- Chuyển tiếp token và correlation ID xuống service đích.
- CORS, giới hạn kích thước request và các rule HTTP dùng chung.
- Rate limit ở giai đoạn sau.
- Ghi access log nhưng không ghi token hoặc dữ liệu nhạy cảm.

Gateway không chịu trách nhiệm:

- Kiểm tra username/password.
- Truy vấn database user cho mỗi request.
- Phát access token hoặc refresh token.
- Quyết định toàn bộ quyền nghiệp vụ của Product/Order Service.
- Lưu access token thay client trong mô hình JWT stateless.

### 3.3. Product Service và Order Service

Các resource service chịu trách nhiệm:

- Verify JWT tại local để tạo trusted principal, nếu áp dụng defense in depth.
- Đọc `userId` từ claim `sub`.
- Đọc role/scope từ claims.
- Áp dụng authorization theo domain của chính service.
- Không gọi User Service chỉ để xác thực lại mỗi request.

Ví dụ:

- Product Service quyết định chỉ `ADMIN` được tạo, sửa hoặc xóa sản phẩm.
- Order Service quyết định `USER` chỉ được xem đơn của chính mình.
- Order Service cho phép `ADMIN` xem mọi đơn hàng.

## 4. Actor

| Actor | Mô tả |
|---|---|
| Guest | Người chưa đăng nhập, được đăng ký, đăng nhập và refresh nếu có refresh token hợp lệ |
| User | Người dùng đã đăng nhập, được xem/cập nhật hồ sơ và thao tác nghiệp vụ của mình |
| Admin | Người dùng có role `ADMIN`, được thực hiện các nghiệp vụ quản trị được service đích cho phép |
| Client application | Web, mobile hoặc API client giữ token và gửi access token theo request |
| API Gateway | Xác thực token tại biên và route request |
| Resource service | Product/Order Service xác minh principal và áp dụng quyền nghiệp vụ |

## 5. Phân biệt authentication và authorization

### Authentication

Authentication trả lời câu hỏi: **Ai đang gửi request?**

ShopCore có hai bước authentication khác nhau:

1. Credential authentication tại User Service:
   - Kiểm tra username/email và password khi login.
   - Kiểm tra trạng thái tài khoản.
   - Phát token sau khi thông tin đăng nhập hợp lệ.
2. Token authentication tại Gateway và resource service:
   - Kiểm tra chữ ký JWT.
   - Kiểm tra thời hạn và các claims bắt buộc.
   - Tạo principal từ `sub` và roles.

Verify JWT tại mỗi service không đồng nghĩa với việc mỗi service gọi lại User Service. Đây chỉ là phép kiểm tra chữ ký tại local, không cần truy vấn database hoặc network call.

### Authorization

Authorization trả lời câu hỏi: **Danh tính này được phép làm gì?**

- Gateway chỉ nên áp dụng rule tổng quát, ví dụ endpoint public hay protected.
- Service đích phải áp dụng rule nghiệp vụ chi tiết.
- User Service quản lý role nhưng không quyết định thay Product/Order Service mọi quyền domain.

## 6. Mô hình dữ liệu User Service

### 6.1. User

| Trường | Ý nghĩa | Rule |
|---|---|---|
| `id` | Định danh user | UUID, immutable |
| `username` | Tên đăng nhập | Unique, normalize trước khi so sánh |
| `email` | Email | Unique, lowercase/normalize trước khi lưu |
| `passwordHash` | Password đã mã hóa | Không bao giờ trả ra API hoặc ghi log |
| `status` | Trạng thái tài khoản | `ACTIVE`, `LOCKED`, `DISABLED` |
| `createdAt` | Thời điểm tạo | UTC, do server quản lý |
| `updatedAt` | Thời điểm cập nhật | UTC, do server quản lý |
| `version` | Phiên bản optimistic locking | Khuyến nghị để tránh lost update |

### 6.2. Role

| Trường | Ý nghĩa |
|---|---|
| `id` | Định danh role |
| `name` | Tên role, trước mắt gồm `USER`, `ADMIN` |

Một user có thể có nhiều role và một role có thể thuộc nhiều user. Database nên có bảng nối `user_roles` với unique constraint trên cặp `(user_id, role_id)`.

### 6.3. Refresh token

| Trường | Ý nghĩa | Rule |
|---|---|---|
| `id` | Định danh record | UUID |
| `userId` | User sở hữu token | Foreign key hoặc logical reference trong cùng service |
| `tokenHash` | Hash của refresh token | Không lưu raw token |
| `expiresAt` | Thời điểm hết hạn | UTC |
| `createdAt` | Thời điểm phát hành | UTC |
| `revokedAt` | Thời điểm thu hồi | Null nếu còn hiệu lực |
| `replacedByTokenId` | Token mới thay thế | Dùng cho refresh-token rotation |

Quan hệ tổng quát:

```text
User ── many-to-many ── Role
  │
  └── one-to-many ── RefreshToken
```

## 7. Trạng thái và vòng đời

### 7.1. User status

| Status | Có thể login | Có thể refresh | Có thể gọi API protected |
|---|---:|---:|---:|
| `ACTIVE` | Có | Có | Có, nếu token hợp lệ |
| `LOCKED` | Không | Không | Token đã phát có thể còn hiệu lực tới khi hết hạn |
| `DISABLED` | Không | Không | Token đã phát có thể còn hiệu lực tới khi hết hạn |

MVP chấp nhận access token cũ sống tới hết TTL. Nếu cần khóa tức thời, hệ thống phải bổ sung blacklist/introspection/token version, đồng nghĩa với thêm state vào luồng validate access token.

### 7.2. Refresh token status

```text
ACTIVE ──refresh──> ROTATED
   │
   ├──logout──────> REVOKED
   │
   └──time────────> EXPIRED
```

Refresh token chỉ hợp lệ khi:

- Tồn tại record tương ứng.
- Chưa hết hạn.
- Chưa bị thu hồi hoặc thay thế.
- User vẫn ở trạng thái `ACTIVE`.

## 8. Use case chi tiết của User Service

### UC-01: Đăng ký tài khoản

**Endpoint:** `POST /api/auth/register`

**Actor:** Guest.

**Input tối thiểu:**

- `username`
- `email`
- `password`

**Luồng chính:**

1. Validate request format.
2. Normalize username và email.
3. Kiểm tra username chưa tồn tại.
4. Kiểm tra email chưa tồn tại.
5. Kiểm tra password policy.
6. Hash password bằng password encoder mạnh.
7. Gán role mặc định `USER`.
8. Gán status mặc định `ACTIVE` cho MVP.
9. Lưu user và role trong một transaction.
10. Trả thông tin public của user.

**Business rule:**

- Client không được tự gán role `ADMIN`.
- Username và email phải có unique constraint ở database.
- Response không chứa password hash.
- Lỗi duplicate do race condition ở database phải được map thành `409 Conflict`.

**Kết quả đề xuất:** `201 Created`.

### UC-02: Đăng nhập

**Endpoint:** `POST /api/auth/login`

**Actor:** Guest.

**Input:** username/email và password.

**Luồng chính:**

1. Tìm user theo username hoặc email đã normalize.
2. Kiểm tra password với password hash.
3. Kiểm tra user ở trạng thái `ACTIVE`.
4. Lấy roles hiện tại của user.
5. Phát access token ngắn hạn.
6. Tạo refresh token ngẫu nhiên đủ mạnh.
7. Lưu hash của refresh token.
8. Trả access token và refresh token cho client.

**Business rule:**

- Thông báo lỗi không tiết lộ user có tồn tại hay không.
- Không ghi password hoặc token vào log.
- Có thể ghi audit event login thành công/thất bại nhưng phải tránh PII không cần thiết.
- Rate limit login nên được bổ sung tại Gateway trong giai đoạn sau.

**Kết quả đề xuất:** `200 OK`; sai credential trả `401 Unauthorized`.

### UC-03: Làm mới token

**Endpoint:** `POST /api/auth/refresh`

**Actor:** Client có refresh token.

**Luồng chính:**

1. Nhận refresh token từ cookie bảo mật hoặc request body theo loại client.
2. Hash token nhận được và tìm record tương ứng.
3. Kiểm tra token còn hiệu lực.
4. Kiểm tra user còn `ACTIVE`.
5. Thu hồi token cũ.
6. Phát access token mới.
7. Phát refresh token mới và lưu hash.
8. Liên kết token cũ với token mới để hỗ trợ phát hiện reuse.

**Business rule:**

- Áp dụng refresh-token rotation.
- Hai request refresh đồng thời chỉ một request được thành công.
- Nếu phát hiện reuse token đã rotate, có thể thu hồi toàn bộ token family và buộc đăng nhập lại.

**Kết quả đề xuất:** `200 OK`; token không hợp lệ trả `401 Unauthorized`.

### UC-04: Đăng xuất

**Endpoint:** `POST /api/auth/logout`

**Actor:** User hoặc client có refresh token.

**Luồng chính:**

1. Xác định refresh token cần thu hồi.
2. Đánh dấu `revokedAt`.
3. Xóa refresh-token cookie nếu sử dụng cookie.
4. Trả kết quả thành công ngay cả khi token đã bị thu hồi để giữ tính idempotent.

**Lưu ý:** Access token JWT đã phát không tự biến mất. Nó tiếp tục hợp lệ cho tới `exp`, vì vậy access-token TTL phải ngắn.

**Kết quả đề xuất:** `204 No Content`.

### UC-05: Xem hồ sơ hiện tại

**Endpoint:** `GET /api/users/me`

**Actor:** User đã đăng nhập.

**Luồng chính:**

1. Lấy `userId` từ principal/JWT claim `sub`.
2. Đọc user từ database.
3. Trả dữ liệu hồ sơ public.

**Business rule:**

- Không nhận `userId` từ query/body cho API `/me`.
- Không trả password hash, refresh token hoặc thông tin bảo mật.

### UC-06: Cập nhật hồ sơ hiện tại

**Endpoint:** `PUT /api/users/me`

**Actor:** User đã đăng nhập.

**Luồng chính:**

1. Lấy `userId` từ JWT.
2. Validate các trường cho phép cập nhật.
3. Kiểm tra uniqueness nếu email/username được phép đổi.
4. Cập nhật trong transaction.
5. Trả hồ sơ mới.

**Business rule:**

- Chỉ whitelist trường được cập nhật.
- Không cho sửa `roles`, `status`, `passwordHash`, `createdAt` qua endpoint này.
- Đổi password nên là một use case/endpoint riêng khi mở rộng.

## 9. Thiết kế token

### 9.1. Access token

Access token là JWT được User Service ký bằng private key. Gateway và các resource service verify bằng public key.

Claims tối thiểu:

```json
{
  "sub": "user-uuid",
  "roles": ["USER"],
  "iss": "shopcore-user-service",
  "aud": ["shopcore-api"],
  "iat": 0,
  "exp": 0,
  "jti": "token-uuid"
}
```

Rule:

- `sub`: user ID ổn định, không dùng email làm identity chính.
- `roles`: dùng cho coarse-grained authorization.
- `iss`: Gateway/service chỉ chấp nhận issuer cấu hình trước.
- `aud`: ngăn token dành cho hệ thống khác bị dùng tại ShopCore.
- `exp`: bắt buộc.
- `jti`: hữu ích cho audit hoặc blacklist khi cần.
- Không đặt password, địa chỉ nhạy cảm hoặc toàn bộ user profile trong token.

TTL đề xuất ban đầu: 5 đến 15 phút. Giá trị chính xác cần đưa vào configuration, không hard-code.

### 9.2. Refresh token

Refresh token nên là chuỗi opaque ngẫu nhiên thay vì JWT, vì User Service luôn phải kiểm tra state của nó trong database.

Rule:

- TTL dài hơn access token, ví dụ 7 đến 30 ngày tùy yêu cầu sản phẩm.
- Lưu hash thay vì raw value.
- Rotate sau mỗi lần sử dụng.
- Có thể thu hồi từng thiết bị hoặc toàn bộ thiết bị.

### 9.3. Nơi giữ token

| Thành phần | Access token | Refresh token |
|---|---|---|
| Client | Giữ và gửi trong mỗi request | Giữ an toàn; web browser ưu tiên cookie `HttpOnly`, `Secure`, `SameSite` |
| API Gateway | Không lưu; chỉ verify trong request hiện tại | Không lưu |
| User Service | Không cần lưu access token | Lưu hash và trạng thái refresh token |
| Resource service | Không lưu; verify trong request hiện tại | Không sử dụng |

Nếu Gateway lưu token/session thay client thì kiến trúc chuyển sang BFF/server-side session, không còn là JWT stateless thuần túy. Đây không phải thiết kế mặc định của ShopCore.

### 9.4. Signing key

Khuyến nghị dùng chữ ký bất đối xứng:

```text
User Service: private key để ký
Gateway: public key để verify
Product/Order Service: public key để verify
```

Không nên chia sẻ một HMAC secret cho tất cả service vì bất kỳ service nào giữ secret cũng có khả năng tự tạo token hợp lệ.

Public key có thể được cấu hình trực tiếp ở MVP. Khi cần key rotation, User Service có thể công bố JWKS endpoint và token header chứa `kid`.

## 10. Luồng end-to-end

### 10.1. Login

```text
Client
  │ POST /api/auth/login
  │ username/email + password
  ▼
API Gateway
  │ route public endpoint
  ▼
User Service
  │ verify credential và status
  │ phát access token + refresh token
  ▼
API Gateway
  │ không lưu token
  ▼
Client giữ token
```

### 10.2. Gọi Product API đã bảo vệ

```text
Client
  │ GET/POST /api/products/**
  │ Authorization: Bearer <access-token>
  ▼
API Gateway
  │ verify signature, exp, iss, aud
  │ request không hợp lệ → 401
  │ request hợp lệ → forward Bearer token
  ▼
Product Service
  │ verify JWT tại local
  │ tạo principal từ sub và roles
  │ kiểm tra quyền nghiệp vụ
  │ không đủ quyền → 403
  ▼
Controller / Use case
```

Gateway không gọi User Service trong luồng này.

### 10.3. Refresh token

```text
Client
  │ POST /api/auth/refresh + refresh token
  ▼
Gateway
  │ route public endpoint
  ▼
User Service
  │ kiểm tra DB state
  │ rotate refresh token
  │ phát access token mới
  ▼
Client
```

### 10.4. Service-to-service

Traffic nội bộ không cần quay lại Gateway:

```text
Order Service ──gRPC──> Product Service
```

Cần phân biệt hai trường hợp:

1. Gọi thay mặt user:
   - Propagate user access token hoặc một user-context token nội bộ có thời hạn ngắn.
   - Service nhận vẫn kiểm tra audience và authorization.
2. Gọi bằng danh tính của service:
   - Dùng service credential/internal token riêng.
   - Không giả lập một user tùy ý.

## 11. Rule tại API Gateway

### 11.1. Public endpoint

Các endpoint không yêu cầu access token:

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

`POST /api/auth/logout` có thể yêu cầu access token hoặc chỉ refresh token hợp lệ tùy contract cuối cùng. Khuyến nghị chấp nhận refresh token để vẫn logout được khi access token vừa hết hạn.

Actuator public exposure cần được giới hạn theo môi trường. Chỉ nên public health information tối thiểu.

### 11.2. Protected endpoint

Các endpoint còn lại mặc định yêu cầu access token, trừ khi có rule explicit khác:

```text
/api/users/**
/api/products/**
/api/categories/**
/api/inventory/**
/api/orders/**
```

Nếu có API đọc catalog công khai, cần whitelist chính xác theo HTTP method và path, không mở toàn bộ `/api/products/**`.

### 11.3. Validation tại Gateway

Gateway phải kiểm tra:

- Header dùng Bearer scheme hợp lệ.
- Chữ ký JWT.
- Thuật toán ký nằm trong allowlist.
- `exp` và nếu có `nbf`.
- `iss` đúng cấu hình.
- `aud` chứa audience phù hợp.
- Claims bắt buộc có kiểu dữ liệu đúng.

Gateway không nên:

- Tin dữ liệu identity do client gửi qua `X-User-Id` hoặc `X-User-Roles`.
- Gọi User Service cho mỗi request chỉ để validate JWT.
- Log header `Authorization`.
- Biến lỗi nội bộ thành thông tin chi tiết lộ security configuration.

### 11.4. Identity propagation

Phương án mặc định được chọn:

- Gateway forward nguyên `Authorization: Bearer <JWT>`.
- Resource service tự verify JWT và tạo principal.

Gateway phải xóa các identity header do client tự gửi, ví dụ:

```text
X-User-Id
X-User-Roles
X-Authenticated-User
```

Nếu sau này chọn mô hình trusted header, bắt buộc phải:

- Chặn truy cập trực tiếp tới resource service.
- Chỉ nhận traffic từ Gateway.
- Dùng mTLS hoặc cơ chế xác thực nội bộ tương đương.
- Để Gateway strip rồi tạo lại identity header.

Trusted header không phải lựa chọn mặc định cho MVP.

## 12. Authorization matrix ban đầu

| Endpoint/operation | Guest | USER | ADMIN | Nơi quyết định cuối |
|---|---:|---:|---:|---|
| Register | Có | Có thể từ chối nếu đã login | Có thể từ chối nếu đã login | User Service |
| Login | Có | Có | Có | User Service |
| Refresh | Có token | Có token | Có token | User Service |
| Logout | Có refresh token | Có | Có | User Service |
| Xem/sửa `/api/users/me` | Không | Có | Có | User Service |
| Đọc catalog public | Tùy quyết định sản phẩm | Có | Có | Product Service + Gateway whitelist |
| Tạo/sửa/xóa sản phẩm | Không | Không | Có | Product Service |
| Tạo order | Không | Có | Có, nếu business cho phép | Order Service |
| Xem order của chính mình | Không | Có | Có | Order Service |
| Xem mọi order | Không | Không | Có | Order Service |

Role trong JWT chỉ là đầu vào cho authorization. Resource service vẫn phải kiểm tra ownership, trạng thái resource và các invariant nghiệp vụ.

## 13. API contract đề xuất

### 13.1. Register request

```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "user-supplied-password"
}
```

Response không chứa token theo mặc định. Nếu muốn auto-login sau register, phải ghi rõ đây là business decision và tái sử dụng cùng cơ chế phát token của login.

### 13.2. Login request/response

```json
{
  "login": "alice@example.com",
  "password": "user-supplied-password"
}
```

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "user-uuid",
    "username": "alice",
    "email": "alice@example.com",
    "roles": ["USER"]
  }
}
```

Với browser, refresh token nên nằm trong `HttpOnly` cookie và không xuất hiện trong JSON response. Với native/non-browser client, cần contract lưu trữ refresh token riêng phù hợp với nền tảng.

### 13.3. Error response chung

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Invalid credentials",
  "status": 401,
  "path": "/api/auth/login",
  "correlationId": "request-uuid",
  "timestamp": "2026-09-29T12:00:00Z"
}
```

Không đưa stack trace, SQL message hoặc chi tiết token validation ra client.

## 14. HTTP status và error code

| Tình huống | HTTP | Error code đề xuất |
|---|---:|---|
| Request không hợp lệ | 400 | `VALIDATION_FAILED` |
| Thiếu access token | 401 | `AUTH_TOKEN_MISSING` |
| Access token sai/hết hạn | 401 | `AUTH_TOKEN_INVALID` |
| Sai username/password | 401 | `AUTH_INVALID_CREDENTIALS` |
| Refresh token sai/hết hạn | 401 | `AUTH_REFRESH_TOKEN_INVALID` |
| Đã authenticate nhưng thiếu quyền | 403 | `AUTH_FORBIDDEN` |
| Username/email đã tồn tại | 409 | `USER_ALREADY_EXISTS` |
| User hiện tại không tồn tại | 404 | `USER_NOT_FOUND` |
| User bị khóa/vô hiệu hóa | 403 | `USER_NOT_ACTIVE` |
| Rate limit vượt ngưỡng | 429 | `RATE_LIMIT_EXCEEDED` |

Quy ước quan trọng:

- `401`: chưa chứng minh được danh tính bằng credential/token hợp lệ.
- `403`: đã có danh tính nhưng không được phép thực hiện hành động.

## 15. Transaction và concurrency

Các operation cần transaction:

- Tạo user và gán role mặc định.
- Rotate refresh token cũ sang token mới.
- Thu hồi toàn bộ refresh token của user.
- Cập nhật profile có kiểm tra version.

Các race condition cần xử lý:

- Hai request đăng ký cùng username/email.
- Hai request refresh đồng thời cùng một token.
- Admin khóa user đúng lúc user refresh token.
- Hai request cập nhật profile ghi đè nhau.

Không chỉ dựa vào check-before-insert; unique constraint và conditional update tại database phải là lớp bảo vệ cuối.

## 16. Security requirements

### Password

- Hash bằng BCrypt hoặc Argon2 thông qua `PasswordEncoder`.
- Không encrypt/decrypt password.
- Không log password.
- Password policy phải nằm trong configuration hoặc validator dùng chung.

### Token

- Chỉ truyền qua HTTPS ở môi trường thật.
- Không đặt token trong query string.
- Không log `Authorization`, cookie refresh token hoặc raw token.
- Validate cố định signing algorithm, không tin `alg` tùy ý từ token.
- Có quy trình rotation signing key.

### Browser security

- Refresh token cookie: `HttpOnly`, `Secure`, `SameSite` phù hợp.
- Nếu refresh/logout dùng cookie, phải phân tích CSRF và bảo vệ tương ứng.
- CORS chỉ allow origin cần thiết; không dùng wildcard với credential.
- Access token ưu tiên giữ trong memory thay vì local storage nếu frontend architecture cho phép.

### Network boundary

- Chỉ Gateway được expose public.
- User/Product/Order Service chỉ lắng nghe trên private network trong deployment thật.
- Internal network không thay thế hoàn toàn token/service authentication.

## 17. Logging, audit và observability

Gateway tạo hoặc propagate `X-Correlation-Id`; các service đưa cùng ID vào log và error response.

Nên audit các sự kiện:

- User đăng ký.
- Login thành công/thất bại.
- Refresh token bị reuse.
- Logout.
- User bị khóa hoặc vô hiệu hóa.
- Role bị thay đổi.

Không audit dưới dạng plaintext:

- Password.
- Access token.
- Refresh token.
- Cookie authentication.

Metric gợi ý:

- Số login thành công/thất bại.
- Số token refresh thành công/thất bại.
- Số request `401`/`403` tại Gateway và từng service.
- Latency theo route.
- Tỷ lệ `429` khi có rate limit.

## 18. Yêu cầu kiểm thử và acceptance criteria

### User Service

- Đăng ký thành công tạo user với role `USER` và password đã hash.
- Không đăng ký được username/email trùng, kể cả hai request đồng thời.
- Login đúng trả access token có claims hợp lệ.
- Login sai luôn trả response chung, không tiết lộ account tồn tại.
- User `LOCKED` hoặc `DISABLED` không login/refresh được.
- Refresh rotation khiến token cũ không thể dùng lần hai.
- Logout idempotent và token đã thu hồi không refresh được.
- `/api/users/me` luôn dùng `sub`, không tin user ID từ client.
- Response không bao giờ serialize password hash hoặc token hash.

### API Gateway

- Public auth endpoints hoạt động không cần access token.
- Protected endpoints trả `401` khi thiếu token.
- Token sai chữ ký, sai issuer/audience hoặc hết hạn bị từ chối.
- Token hợp lệ được forward tới đúng service.
- Gateway không gọi User Service trong mỗi request protected.
- Header identity giả do client gửi bị loại bỏ.
- `Authorization` không xuất hiện trong log.
- CORS và preflight hoạt động đúng với origin được phép.

### Resource services

- JWT hợp lệ tạo đúng principal từ `sub`.
- USER gọi API chỉ dành cho ADMIN nhận `403`.
- Request bypass Gateway vẫn không được chấp nhận nếu token không hợp lệ.
- Ownership rule dùng `sub`, không dùng user ID do client tự khai báo.

## 19. Trình tự triển khai đề xuất

### Giai đoạn 1: User data và credential authentication

1. Thêm migration cho `users`, `roles`, `user_roles`, `refresh_tokens`.
2. Thêm Spring Security và password encoder.
3. Implement register và login.
4. Tạo key pair và JWT issuer.
5. Viết integration test cho credential flow.

### Giai đoạn 2: Token lifecycle

1. Implement refresh-token rotation.
2. Implement logout/revoke.
3. Implement `/api/users/me`.
4. Bổ sung concurrency test cho refresh.

### Giai đoạn 3: Gateway authentication

1. Cấu hình Gateway thành OAuth2 Resource Server/JWT validator.
2. Khai báo public và protected routes.
3. Forward Bearer token.
4. Strip untrusted identity headers.
5. Chuẩn hóa `401`, correlation ID và security logging.

### Giai đoạn 4: Resource-service authorization

1. Cấu hình Product/Order Service verify cùng issuer/public key.
2. Map claims sang authorities.
3. Implement method/endpoint authorization.
4. Implement ownership rule trong Order Service.
5. Kiểm thử end-to-end qua Gateway và kiểm thử bypass trực tiếp.

### Giai đoạn 5: Hardening

1. Rate limit login/refresh.
2. Key rotation/JWKS.
3. Audit và security metrics.
4. Detection refresh-token reuse.
5. Cân nhắc MFA, email verification và forgot-password khi có yêu cầu.

## 20. Ngoài phạm vi MVP

Các chức năng sau chưa cần trong phiên bản đầu:

- OAuth login qua Google/Facebook.
- Multi-factor authentication.
- Email verification.
- Forgot/reset password.
- Permission động chi tiết ngoài `USER`/`ADMIN`.
- Session dashboard cho nhiều thiết bị.
- Blacklist mọi access token.
- BFF/server-side session.
- Service mesh authentication.

Các chức năng này chỉ nên thêm khi có yêu cầu nghiệp vụ cụ thể.

## 21. Quyết định kiến trúc mặc định

Nếu chưa có quyết định khác, ShopCore áp dụng các lựa chọn sau:

1. User Service sở hữu credential, user, role và refresh token.
2. User Service xác thực username/password và phát token.
3. Client giữ access token và refresh token; Gateway không lưu token.
4. Gateway validate access token tại biên.
5. Gateway forward Bearer token tới resource service.
6. Mỗi resource service verify JWT tại local, không gọi User Service.
7. Resource service tự quyết định authorization theo domain.
8. Access token dùng chữ ký bất đối xứng và TTL ngắn.
9. Refresh token là opaque token, lưu hash và rotate mỗi lần dùng.
10. Client traffic đi qua Gateway; service-to-service traffic đi trực tiếp qua giao thức nội bộ.

Ranh giới cuối cùng có thể tóm tắt như sau:

```text
User Service   = xác thực credential + phát token + quản lý identity
API Gateway    = xác thực token tại biên + routing + policy HTTP dùng chung
Product/Order  = xác minh principal + authorization theo nghiệp vụ
Client         = giữ token và gửi access token trên mỗi request
```
