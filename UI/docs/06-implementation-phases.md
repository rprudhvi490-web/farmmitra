# Implementation Phases — Angular Web UI

## Phase Overview

| Phase | Name | Status |
|-------|------|--------|
| 9.1 | Setup & Core Infrastructure | ✅ Complete |
| 9.2 | Auth Flow (OTP + Password Login) | ✅ Complete |
| 9.3 | Customer — Browse & Order | ✅ Complete |
| 9.4 | Customer — Orders, Profile & Password | ✅ Complete |
| 9.5 | Admin — Core Operations | ✅ Complete |
| 9.6 | Admin — Procurement & Transport | ✅ Complete |
| 9.7 | Admin — Delivery & Users | ✅ Complete |
| 9.8 | Notifications, Referral | ✅ Complete |
| 9.9 | Polish, Theme, Deploy | 🟡 In Progress |

---

## Phase 9.1 — Setup & Core Infrastructure ✅

- [x] Angular 21.2.11 workspace under `C:\WeekendBasket\UI`
- [x] Angular Material — FarmMitra green/orange theme
- [x] Nunito font — warm, rounded, farm-to-table feel
- [x] CSS design token system (`--fm-*` variables in `styles.scss`)
- [x] `jwt-decode`, proxy, environments configured
- [x] `AuthInterceptor`, `ErrorInterceptor`, `TokenService`, `AuthService`
- [x] Functional guards: `authGuard`, `roleGuard`, `guestGuard`
- [x] Lazy loading with `loadComponent()`
- [x] `CartService` — BehaviorSubject state
- [x] Global SCSS — status chip classes, utility classes, Material overrides

---

## Phase 9.2 — Auth Flow ✅

- [x] Login screen — two tabs: OTP and Password
- [x] OTP tab — phone input, send OTP, navigate to verify screen
- [x] Password tab — phone + password, show/hide toggle
- [x] JWT stored in localStorage, roles decoded, role-based redirect
- [x] Username persisted to localStorage on login (`wb_username`)
- [x] Logout — calls backend, clears token + username

**APIs:** `POST /auth/send-otp`, `POST /auth/verify-otp`, `POST /auth/login`, `POST /auth/logout`

---

## Phase 9.3 — Customer: Browse & Order ✅

- [x] Customer layout — green navbar, cart badge, notification bell, profile menu
- [x] Username shown below profile icon (falls back to "Guest")
- [x] Home — cycle status card, live countdown timer
- [x] Products — category filter chips, product grid, search (debounced)
- [x] Cart — item list, quantity edit, remove, total
- [x] Checkout — order summary, COD info, notes, place order

**APIs:** `GET /cycles/current`, `GET /categories`, `GET /products`, `POST /orders`

---

## Phase 9.4 — Customer: Orders, Profile & Password ✅

- [x] My Orders — list with status badges, hover lift effect
- [x] Order Detail — items, transport timeline (dot + line), cancel button
- [x] Profile — edit name, flat, block, email (2-column grid)
- [x] Set Password on profile — `PUT /auth/set-password`
- [x] Password login tab on login screen — `POST /auth/login` (7-day token)

**APIs:** `GET /orders/my`, `GET /orders/my/:id`, `PUT /orders/:id/cancel`, `GET /users/me`, `PUT /users/me`, `PUT /auth/set-password`

---

## Phase 9.5 — Admin: Core Operations ✅

- [x] Admin layout — dark green sidenav with orange active-link accent border
- [x] Dashboard — 3 tabs: Overview, Cycle History, Customer Loyalty
- [x] Cycle History — all-time totals + per-cycle revenue generated/collected/pending
- [x] Customer Loyalty — loyalty tag breakdown (NEW/REGULAR/LOYAL/CHAMPION) + customer table
- [x] Cycles — list, create, open/close/status (smart button visibility)
- [x] Products — full CRUD, availability toggle
- [x] Categories — full CRUD

**APIs:** `GET /dashboard/summary`, `GET /dashboard/history`, `GET /dashboard/customers`, `GET /cycles`, `POST /cycles`, `PUT /cycles/:id/open`, `PUT /cycles/:id/close`, `PUT /cycles/:id/status`

---

## Phase 9.6 — Admin: Procurement & Transport ✅

- [x] Orders — cycle filter, status update, payment status (Mark Paid)
- [x] Procurement — sheet table, inline vendor/qty edit, Excel export (download only — no re-upload)
- [x] Transport — sequential stage timeline (only next valid stage shown)

**APIs:** `GET /orders/cycle/:cycleId`, `PUT /orders/:id/status`, `PUT /orders/:id/payment`, `GET /procurement/:cycleId`, `PUT /procurement/items/:productId`, `GET /procurement/:cycleId/export`, `GET /transport/:cycleId`, `POST /transport/:cycleId/stage`

---

## Phase 9.7 — Admin: Delivery & Users ✅

- [x] Delivery Batches — create batch, assign staff, mark delivered
- [x] Order status shown per batch order (✓ Delivered / Mark Delivered button)
- [x] Unassigned orders filter — already-batched orders excluded from dropdown
- [x] Multiple order selection when creating batch
- [x] Users — list, search, block/unblock, assign role

---

## Phase 9.8 — Notifications & Referral ✅

- [x] Notification bell in customer navbar with unread count badge (orange, green border)
- [x] Notifications screen — list, mark read, mark all read
- [x] Referral screen — show code, copy button, history
- [x] Admin Notifications — broadcast to all users + send to specific user by ID

---

## Phase 9.9 — Polish & Deploy 🟡 In Progress

- [x] FarmMitra brand theme — Nunito font, CSS token system, consistent across all screens
- [x] Transport timeline on customer order detail
- [x] Transport stages sequential — only next valid stage shown
- [x] Cycle buttons smart — no Open for PROCUREMENT/DELIVERING
- [x] Admin broadcast notifications UI complete
- [x] `_redirects` file for Netlify deep linking
- [x] CORS config in backend (env-var driven)
- [ ] Responsive layout — mobile breakpoints
- [ ] Deploy to Netlify (Angular) + Render (Spring Boot)

---

## End-to-End Flow Checklist

- [x] Customer registers with OTP
- [x] Customer can set password for future logins
- [x] Customer browses products by category
- [x] Customer adds to cart and places order
- [x] Customer sees order in My Orders with status
- [x] Customer sees transport stages on order detail
- [x] Admin opens/closes weekly cycle
- [x] Admin views and updates orders
- [x] Admin marks orders as paid (COD collection)
- [x] Admin views procurement sheet after cycle closes
- [x] Admin updates transport stages (sequential only)
- [x] Admin creates delivery batches and assigns staff
- [x] Delivery staff marks orders as delivered
- [x] Customer receives notification on delivery
- [x] Admin broadcasts announcements
- [ ] Referral discount applied correctly on order (backend ready, needs e2e test)

---

## Backend Issues Fixed During Phase 9

| Issue | Fix |
|-------|-----|
| `LazyInitializationException` everywhere | `@Transactional(readOnly = true)` on all read methods |
| `lower(bytea)` PostgreSQL 18 | `ILIKE` instead of `LOWER()` |
| `POST /cycles` missing | Added endpoint + DTO |
| `PUT /cycles/:id/status` wrong param | `@RequestBody` instead of `@RequestParam` |
| `GET /products` lazy category | `JOIN FETCH p.category` |
| `GET /products/all` missing | Added admin endpoint |
| Admin orders wrong URL | Fixed to `/orders/cycle/:cycleId` |
| Batch `orderId` undefined | Fixed `BatchOrder` interface field name |
| Batch delivered not showing | Added `orderStatus` to `BatchOrderSummary` DTO |
| State machine bypass | Auto-advance via `apply()` for each step |
| Payment status missing | Added `PUT /orders/:id/payment` endpoint |
| Transport skip stages | Sequential filter — only next valid stages shown |
| AOP not logging errors | Added `LoggingAspect` + fixed `GlobalExceptionHandler` |
| CORS blocked in production | Added `CorsConfigurationSource` bean, env-var driven |
| Username not shown in navbar | `saveUsername()` in `TokenService`, shown below profile icon |
