# User Service Implementation Roadmap

Tài liệu này là checklist triển khai chính cho `user-service`. Phân tích nghiệp vụ chi tiết nằm tại [USER_SERVICE_AND_API_GATEWAY_BUSINESS_ANALYSIS.md](USER_SERVICE_AND_API_GATEWAY_BUSINESS_ANALYSIS.md).

## Quy ước checkpoint

- Chỉ đánh `[x]` khi toàn bộ task và điều kiện nghiệm thu của checkpoint đã đạt.
- Ghi ngày và bằng chứng kiểm thử vào dòng `Evidence`.
- Phạm vi không bao gồm thay đổi API Gateway, Product Service hoặc Order Service.

## CP0 — Contract và baseline

- [x] Baseline `user-service` test pass trước khi triển khai.
- [x] Khóa API: register, login, refresh, logout, get/update current profile.
- [x] Khóa role `USER`, `ADMIN` và status `ACTIVE`, `LOCKED`, `DISABLED`.
- [x] Liên kết tài liệu phân tích nghiệp vụ và ghi rõ phạm vi.

**Điều kiện hoàn thành:** Contract không còn quyết định mở.

**Evidence:** 2026-09-29 — `.\mvnw.cmd -pl user-service -am test -q` pass trên baseline.

## CP1 — Dependency, configuration và Flyway

- [ ] Thêm Spring Security, Resource Server/Jose và Flyway PostgreSQL.
- [ ] Giữ `ddl-auto=validate`; Flyway quản lý schema.
- [ ] Thêm typed properties cho JWT, refresh token, cookie và allowed origins.
- [ ] Tạo/seed schema `users`, `roles`, `user_roles`, `refresh_tokens`.
- [ ] Bật Flyway trong test profile H2 PostgreSQL mode.

**Điều kiện hoàn thành:** Context khởi động, Flyway migrate và Hibernate validate thành công.

**Lệnh kiểm tra:** `.\mvnw.cmd -pl user-service -am test`

**Evidence:** Chưa có.

## CP2 — Domain và persistence

- [ ] Implement entity, enum và repository.
- [ ] UUID sinh tại application; timestamp dùng UTC.
- [ ] Normalize username/email; chỉ lưu password hash và refresh-token hash.
- [ ] Hỗ trợ token family, replacement, revoke và expiry.
- [ ] Khóa refresh token khi rotate và optimistic locking cho user.

**Điều kiện hoàn thành:** Persistence tests chứng minh mapping, uniqueness và locking.

**Lệnh kiểm tra:** `.\mvnw.cmd -pl user-service -Dtest=*Persistence* test`

**Evidence:** Chưa có.

## CP3 — Register

- [ ] Validate username 3–50, email tối đa 254, password 8–72.
- [ ] BCrypt password; role/status mặc định là `USER`/`ACTIVE`.
- [ ] Không nhận role/status từ client.
- [ ] Duplicate trả `409 USER_ALREADY_EXISTS`.
- [ ] Thành công trả `201` và user public DTO.

**Điều kiện hoàn thành:** Happy path, validation và duplicate tests pass.

**Evidence:** Chưa có.

## CP4 — Login và access JWT

- [ ] Login bằng username hoặc email; lỗi credential thống nhất.
- [ ] Chặn account `LOCKED`/`DISABLED`.
- [ ] Phát JWT RS256 15 phút với claims đã khóa.
- [ ] Trả access token trong JSON và refresh token trong cookie bảo mật.
- [ ] Cung cấp JWKS public contract và script tạo key local.

**Điều kiện hoàn thành:** JWT verify được bằng public key, claims và cookie flags đúng.

**Evidence:** Chưa có.

## CP5 — Refresh và logout

- [ ] Refresh chỉ đọc cookie, TTL mặc định 7 ngày và rotate mỗi lần.
- [ ] Replay/reuse revoke toàn bộ token family.
- [ ] Hai refresh đồng thời chỉ một request thành công.
- [ ] Logout idempotent, revoke token, xóa cookie và trả `204`.
- [ ] Bảo vệ Origin cho refresh/logout; cookie dùng `SameSite=Strict`.

**Điều kiện hoàn thành:** Rotation, replay, race, expiry và logout tests pass.

**Evidence:** Chưa có.

## CP6 — Profile và self-authentication

- [ ] User Service verify access JWT bằng public key.
- [ ] `/api/users/me` chỉ dùng claim `sub`.
- [ ] Chỉ cho đổi username/email; không nhận trường bảo mật.
- [ ] Chuẩn hóa `401`, `403`, `404 USER_NOT_FOUND`.

**Điều kiện hoàn thành:** Không thể đọc/sửa user khác bằng ID do client khai báo.

**Evidence:** Chưa có.

## CP7 — Error handling và hardening

- [ ] Error response chuẩn và global exception handler.
- [ ] Correlation ID được tạo/propagate.
- [ ] SecurityFilterChain chỉ public auth endpoints, JWKS và health tối thiểu.
- [ ] Không log secret; có audit event không chứa secret.

**Điều kiện hoàn thành:** Security/error tests pass và log không lộ secret.

**Evidence:** Chưa có.

## CP8 — Kiểm thử và hoàn tất

- [ ] Unit/integration/security tests bao phủ toàn bộ sáu endpoint.
- [ ] Module `test` và `verify` pass.
- [ ] PostgreSQL 17 smoke test pass.
- [ ] Full reactor `clean verify` pass.
- [ ] README có hướng dẫn key, chạy service và gọi API.
- [ ] Tất cả checkpoint trước đã hoàn thành.

**Lệnh kiểm tra:**

```powershell
.\mvnw.cmd -pl user-service -am test
.\mvnw.cmd -pl user-service -am verify
docker compose up -d postgres-user
.\mvnw.cmd clean verify
```

**Evidence:** Chưa có.

## Contract đã khóa

- Register trả user DTO, không tự login.
- Login trả access token JSON và set `shopcore_refresh` HttpOnly cookie.
- Refresh rotate cookie; logout không yêu cầu access token.
- `/api/users/me` dùng Bearer JWT và claim `sub`.
- RS256; access TTL 15 phút; refresh TTL 7 ngày.
- MVP không gồm verification email, reset password, MFA, social login, admin API, rate limit hoặc access-token blacklist.
