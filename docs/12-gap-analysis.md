# Gap Analysis — ElderCare Patterns vs WeekendBasket

## Purpose

This document maps every pattern from ElderCare to WeekendBasket — identifying what carries over directly, what needs adaptation, and what is brand new.

---

## Pattern Comparison Table

| Pattern | ElderCare | WeekendBasket | Gap / Action |
|---------|-----------|---------------|--------------|
| Spring Boot version | 3.3.x | 3.3.x | ✅ Same |
| Java version | 17 | 17 | ✅ Same |
| JWT library | jjwt 0.12.x | jjwt 0.12.x | ✅ Same |
| Auth method | Username + BCrypt password | Phone + OTP | 🔴 New — OtpService needed |
| JwtFilter | OncePerRequestFilter | OncePerRequestFilter | ✅ Same pattern |
| JwtUtil | generate/validate/extract | generate/validate/extract | ✅ Same pattern |
| SecurityConfig | RoleHierarchy + filter chain | RoleHierarchy + filter chain | ✅ Same pattern |
| Role hierarchy | SUPER_ADMIN > ADMIN > NURSE/DOCTOR | SUPER_ADMIN > ADMIN > DELIVERY/CUSTOMER | ✅ Same pattern, different roles |
| BaseEntity | @MappedSuperclass audit cols | @MappedSuperclass audit cols | ✅ Same |
| GlobalExceptionHandler | @RestControllerAdvice | @RestControllerAdvice | ✅ Same pattern |
| Custom exceptions | ElderCareException | WeekendBasketException | ✅ Same pattern, rename |
| Master table | 9 types, lookup_code pattern | 7 types, same pattern | ✅ Same pattern |
| Audit log (async) | AuditService fire-and-forget | Not needed (different domain) | ⚪ Not applicable |
| Token blacklist | invalidated_token table | invalidated_token table | ✅ Same |
| Token cleanup scheduler | Daily cron | Daily cron | ✅ Same |
| Log4j2 | log4j2.xml | log4j2.xml | ✅ Same |
| Actuator | health + mappings | health + mappings | ✅ Same |
| Swagger/OpenAPI | Bearer auth config | Bearer auth config | ✅ Same |
| @Valid input validation | @NotBlank etc. | @NotBlank etc. | ✅ Same |
| DB | MySQL 8 | PostgreSQL (Neon) | 🟡 Dialect change only |
| Hosting | Local / own server | Render free tier | 🟡 Sleep mode consideration |
| Pagination | Planned | Planned | ✅ Same |

---

## New Patterns in WeekendBasket (Not in ElderCare)

| Feature | Description | Complexity |
|---------|-------------|------------|
| OTP Authentication | Phone + OTP instead of password | Medium |
| Weekly Cycle Scheduler | Auto open/close ordering window | Medium |
| Order Aggregation | Sum order items → procurement sheet | Medium |
| Excel Export | Apache POI procurement sheet export | Medium |
| FCM Push Notifications | Firebase Admin SDK integration | Medium |
| In-App Notifications | notification table + read tracking | Low |
| Transport Stage Tracking | Append-only stage log | Low |
| Delivery Batch Management | Group orders → assign staff | Medium |
| Referral System | Referral code tracking | Low |
| Product Availability Toggle | Weekly on/off per product | Low |
| Countdown Timer API | `timeRemainingSeconds` in cycle response | Low |
| Cloudinary Image URLs | URL stored, no upload in backend | Low |

---

## Patterns Carried Over Directly (Zero Rework)

These can be copy-adapted from ElderCare with only naming changes:

1. `BaseEntity` → same `@MappedSuperclass` with audit columns
2. `JwtUtil` → same generate/validate/extract logic (jjwt 0.12.x)
3. `JwtFilter` → same `OncePerRequestFilter` pattern
4. `SecurityConfig` → same structure, different roles in hierarchy
5. `GlobalExceptionHandler` → same `@RestControllerAdvice` shape
6. `AppUserDetailsService` → same pattern, loads from `app_user` + `role_access`
7. `RoleController` → same ROLE_ auto-prepend logic
8. `MasterController` → same GET/POST/PUT pattern
9. `invalidated_token` cleanup scheduler → same cron pattern
10. `log4j2.xml` → same config
11. `application.properties` structure → same, different values

---

## Key Differences to Handle

### 1. Auth — OTP vs Password

ElderCare:
```
POST /auth/login → username + password → BCrypt verify → JWT
```

WeekendBasket:
```
POST /auth/send-otp → phone → generate OTP → store in otp_verification
POST /auth/verify-otp → phone + OTP → validate → JWT
```

New components needed:
- `OtpService` (generate, store, verify, expiry, attempt tracking)
- `otp_verification` table
- `OtpExpiredException`, `OtpMaxAttemptsException`
- No `BCryptPasswordEncoder` needed

---

### 2. User Entity — app_user vs user

ElderCare `user`:
- `username` (unique)
- `password` (BCrypt)
- `user_type`

WeekendBasket `app_user`:
- `phone_number` (unique)
- No password
- `status` (ACTIVE/BLOCKED)

`AppUserDetailsService.loadUserByUsername()` → loads by phone number (username = phone in JWT sub claim).

---

### 3. Database — PostgreSQL vs MySQL

Only change: dialect in `application.properties`.
Spring Boot 3.x auto-detects PostgreSQL — no explicit dialect needed.
All JPA/Hibernate code stays identical.

```properties
# ElderCare (MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/ElderCareDB

# WeekendBasket (PostgreSQL / Neon)
spring.datasource.url=jdbc:postgresql://<neon-host>/weekendbasketdb
```

---

### 4. Scheduler — New in WeekendBasket

ElderCare scheduler: only token cleanup + missed medication slots.

WeekendBasket schedulers:
- `WeeklyOrderScheduler` — open cycle Monday, close Wednesday
- `OtpCleanupScheduler` — delete expired OTP rows
- `TokenCleanupScheduler` — delete expired JWT blacklist rows

---

### 5. FCM — New in WeekendBasket

ElderCare has no push notifications.

WeekendBasket needs:
- Firebase Admin SDK dependency
- `FcmService` — send to single user, send broadcast
- `fcm_token` table management
- Notification triggers on key events

---

## Implementation Priority (based on gap analysis)

| Priority | Component | Source |
|----------|-----------|--------|
| 1 | BaseEntity, exceptions, GlobalExceptionHandler | Carry from ElderCare |
| 2 | JwtUtil, JwtFilter, SecurityConfig, RoleHierarchy | Carry from ElderCare |
| 3 | OtpService, otp_verification, AuthController | New |
| 4 | AppUserDetailsService, UserController | Adapt from ElderCare |
| 5 | Category, Product, MasterController | Adapt from ElderCare |
| 6 | WeeklyCycle, WeeklyOrderScheduler | New |
| 7 | Order, OrderItem, OrderService | New |
| 8 | ProcurementService, Excel export | New |
| 9 | TransportTracking | New |
| 10 | DeliveryBatch | New |
| 11 | FCM, NotificationService | New |
| 12 | Referral | New |
| 13 | Dashboard stats | New |
