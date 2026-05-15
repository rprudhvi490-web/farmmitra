# Implementation Plan — FarmMitra Backend

## Phase 1 — Project Skeleton ✅

- [x] Spring Boot 3.3.x project (Maven, Java 17, JAR)
- [x] Dependencies: web, security, jpa, postgresql, lombok, jjwt 0.12.x, log4j2, actuator
- [x] `application.properties` (DB, JWT secret, port, CORS, FCM)
- [x] `BaseEntity` with audit columns (`@MappedSuperclass`)
- [x] `WeekendBasketException` + `ResourceNotFoundException`
- [x] `GlobalExceptionHandler` (`@RestControllerAdvice`)
- [x] `HealthController` → `GET /api/health`
- [x] Actuator config (`health`, `mappings`)
- [x] Log4j2 configuration (`log4j2.xml`)

## Phase 2 — Auth (OTP + JWT) ✅

- [x] `app_user`, `role`, `role_access`, `otp_verification`, `invalidated_token` entities + repositories
- [x] `OtpService` (generate, store, verify OTP — dev log + SNS strategies)
- [x] `AppUserDetailsService` (loads user + roles from DB)
- [x] `JwtUtil` (generate, validate, extract — jjwt 0.12.x)
- [x] `JwtFilter` (OncePerRequestFilter + token blacklist check)
- [x] `SecurityConfig` (filter chain + RoleHierarchy + CORS)
- [x] `AuthController` → send-otp, verify-otp, login (password), logout, set-password
- [x] `UserController` → profile get/update, admin user management, block/unblock
- [x] `RoleController` → list, create roles (ADMIN)
- [x] OTP cleanup scheduler
- [x] Rate limiting filter (IP-based + per-phone)

## Phase 3 — Product Catalogue ✅

- [x] `community` entity + repository + controller (SUPER_ADMIN)
- [x] `category` entity + repository + service + controller (full CRUD)
- [x] `product` entity + repository + service + controller (full CRUD + availability toggle)
- [x] Cloudinary image URL storage (URL passed from client — no upload in backend)
- [x] `master_table` entity + controller (GET/POST/PUT)

## Phase 4 — Weekly Cycle & Orders ✅

- [x] `weekly_cycle` entity + repository + service + controller
- [x] `WeeklyOrderScheduler` — auto open Monday 00:00, auto close Wednesday 14:00
- [x] `customer_order` + `order_item` entities + repositories
- [x] `OrderService` — place order, validate cycle open, compute totals, referral discount
- [x] `OrderController` — customer + admin endpoints, cancel, mark paid
- [x] Order placement rules: `CycleOpenRule`, `DuplicateOrderRule`, `MinQtyRule`, `ProductAvailableRule`
- [x] Discount strategies: `ReferralDiscountStrategy`, `NoDiscountStrategy`

## Phase 5 — Procurement & Transport ✅

- [x] `procurement_sheet` entity + repository + service
- [x] Aggregation logic: sum order items by product when cycle closes (event-driven)
- [x] `ProcurementController` — view sheet, update vendor/qty/status, export Excel (Apache POI)
- [x] `transport_tracking` entity + repository + service + controller
- [x] Transport stage update triggers in-app + FCM notification to all cycle customers
- [x] Sequential stage enforcement (cannot skip stages)

## Phase 6 — Delivery ✅

- [x] `delivery_batch` + `delivery_batch_order` entities + repositories
- [x] `DeliveryBatchService` — create batch, assign orders, assign staff, mark delivered
- [x] `DeliveryController` — batch management + mark delivered
- [x] Order status auto-advance: PLACED → CONFIRMED → PACKED → DELIVERED
- [x] Auto-trigger transport stages on batch creation and staff assignment
- [x] `BatchCompletedEvent` → auto-complete cycle when all batches DONE

## Phase 7 — Notifications & Referrals ✅

- [x] `notification` + `fcm_token` entities + repositories
- [x] `NotificationService` — in-app + FCM push (strategy pattern)
- [x] FCM integration (Firebase Admin SDK — `FcmPushStrategy`)
- [x] `NotificationController` — my notifications, mark read, mark all read, broadcast, send to user
- [x] `FcmController` — register/remove FCM token
- [x] `referral` entity + repository + service + controller
- [x] Referral code auto-generated on signup, applied at OTP verify

## Phase 8 — Admin Dashboard & Polish ✅

- [x] `DashboardController` — weekly stats, cycle summary, cycle history, customer analytics
- [x] Cycle history: per-cycle revenue generated vs collected vs pending, unique customers
- [x] Customer analytics: loyalty tags (NEW / REGULAR / LOYAL / CHAMPION) by cycles participated
- [x] Input validation (`@Valid` + `@NotBlank` etc.)
- [x] Swagger/OpenAPI with bearer auth
- [x] Token cleanup scheduler
- [x] Invalidated token cleanup scheduler
- [x] `LoggingAspect` — AOP request/response logging

---

## Current Status

| Phase | Name | Status |
|-------|------|--------|
| 1 | Project Skeleton | ✅ Complete |
| 2 | Auth (OTP + JWT) | ✅ Complete |
| 3 | Product Catalogue | ✅ Complete |
| 4 | Weekly Cycle & Orders | ✅ Complete |
| 5 | Procurement & Transport | ✅ Complete |
| 6 | Delivery | ✅ Complete |
| 7 | Notifications & Referrals | ✅ Complete |
| 8 | Admin Dashboard & Polish | ✅ Complete |
| 9 | Angular Web UI | 🟡 Phase 9.9 Polish remaining |
| 10 | React Native Mobile | ⏳ Planned |

---

## Phase 9 — Angular Web Application 🟡

- [x] Angular 21.2.11 workspace under `UI/`
- [x] Core: HTTP interceptor (JWT), auth guard, role guard, error interceptor
- [x] Auth: OTP login + password login, username persisted to localStorage
- [x] Customer: home (countdown), products (search + filter), cart, checkout, orders, order detail (transport timeline), profile, referral, notifications
- [x] Admin: dashboard (overview + cycle history + customer loyalty tabs), cycles, products, categories, orders (mark paid), procurement (inline edit + Excel export), transport (sequential stages), delivery batches, users, notifications (broadcast + direct)
- [x] FarmMitra theme: Nunito font, CSS design token system, consistent elevation + spacing
- [ ] Responsive layout — mobile breakpoints
- [ ] Production build verified
- [ ] Deploy to Netlify + Render

## Phase 10 — React Native Mobile Application ⏳

- [ ] React Native workspace setup
- [ ] Reuse all API contracts validated in Phase 9
- [ ] Android + iOS builds
- [ ] FCM push notification integration
- [ ] App store deployment
