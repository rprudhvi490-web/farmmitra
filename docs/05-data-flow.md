# Data Flow — Table by Table

## 1. app_user + user_profile

**Purpose:** Phone-based user identity and profile.

**Flow:**
1. Customer opens app → enters phone number → `POST /api/auth/send-otp`
2. OTP stored in `otp_verification` with 5-min expiry.
3. Customer enters OTP → `POST /api/auth/verify-otp`
4. If new user: `app_user` row created, `user_profile` row created (empty), `ROLE_CUSTOMER` assigned via `role_access`.
5. JWT token returned with `isNewUser: true` → app redirects to profile completion.
6. Customer fills profile → `PUT /api/users/me` updates `user_profile`.

**Key rule:** `phone_number` is the unique identity. No username/password.

---

## 2. role + role_access

**Purpose:** Define security roles and assign them to users.

**Flow:**
1. 4 standard roles seeded at startup.
2. At OTP verification, new users auto-assigned `ROLE_CUSTOMER`.
3. Admin users created manually by SUPER_ADMIN with `ROLE_ADMIN`.
4. At every request, `AppUserDetailsService` loads user → joins `role_access` → `role` → builds `GrantedAuthority` list.
5. JWT carries roles as claims.
6. Role hierarchy in `SecurityConfig` determines inheritance.

---

## 3. otp_verification

**Purpose:** Temporary OTP storage for phone auth.

**Flow:**
1. `send-otp` → generate 6-digit OTP → save row with `expires_at = now + 5min`, `attempts = 0`.
2. `verify-otp` → find row by phone → check `verified = false`, `expires_at > now`, `attempts < 3`.
3. On wrong OTP → increment `attempts`. On 3rd failure → lock (return 429).
4. On correct OTP → set `verified = true` → proceed to JWT generation.
5. Cleanup scheduler deletes verified/expired rows daily.

---

## 4. community

**Purpose:** Gated community registry. Scopes users and orders.

**Flow:**
1. SUPER_ADMIN creates community via `POST /api/communities`.
2. User profile links to `community_id` during profile setup.
3. MVP: single community. Multi-community: filter all queries by `community_id`.

---

## 5. category + product

**Purpose:** Product catalogue with weekly availability toggle.

**Flow:**
1. Admin creates categories (`POST /api/categories`) and products (`POST /api/products`).
2. Product images uploaded to Cloudinary → URL stored in `product.image_url`.
3. Each week, admin toggles `product.available = true/false` for that week's stock.
4. Customer app calls `GET /api/products` → only returns `available = true` products.

---

## 6. weekly_cycle

**Purpose:** Controls the ordering window for each week.

**Flow:**
1. Scheduler auto-creates a new cycle every Monday at 00:00 with `status = OPEN`.
2. Scheduler auto-closes on Wednesday at 14:00 → `status = CLOSED`.
3. Admin can override: `PUT /api/cycles/{id}/open` or `/close`.
4. Status progression: `OPEN → CLOSED → PROCUREMENT → DELIVERING → COMPLETED`.
5. `GET /api/cycles/current` returns the active cycle — used by app to show countdown timer.

---

## 7. customer_order + order_item

**Purpose:** Customer orders placed during the open cycle window.

**Flow:**
1. Customer calls `POST /api/orders` with items + delivery slot (SAT/SUN).
2. Service validates: cycle is OPEN, products are available, quantities ≥ min_order_qty.
3. `customer_order` row created with system-generated `order_number` (WB-2025-0001).
4. `order_item` rows created for each product line.
5. `total_amount` computed from `order_item.total_price` sum.
6. Order status: `PLACED → CONFIRMED → PACKED → DELIVERED`.
7. Customer can cancel only while cycle is OPEN.

---

## 8. procurement_sheet

**Purpose:** Aggregated demand per product for bulk purchasing.

**Flow:**
1. When cycle closes → service aggregates all `order_item` rows grouped by `product_id`.
2. One `procurement_sheet` row per product per cycle: `total_quantity = SUM(order_item.quantity)`.
3. Admin views via `GET /api/procurement/{cycleId}` — sees all products + total quantities needed.
4. Admin fills in `vendor_name`, `vendor_notes`, `procured_qty` as purchasing happens.
5. Admin exports via `GET /api/procurement/{cycleId}/export` → Apache POI generates Excel sheet.

---

## 9. transport_tracking

**Purpose:** Append-only log of transport stages from Kadapa to Hyderabad.

**Flow:**
1. Admin adds stage updates via `POST /api/transport/{cycleId}/stage`.
2. Stages: `PROCUREMENT_STARTED → GOODS_LOADED → IN_TRANSIT → ARRIVED → PACKING → DISPATCHED`.
3. Each stage update triggers a push notification to all customers with orders in that cycle.
4. Customer app shows "Order Tracking" screen using these stages.

---

## 10. delivery_batch + delivery_batch_order

**Purpose:** Group orders into delivery batches for weekend delivery.

**Flow:**
1. Admin creates batches (`POST /api/batches`) — e.g. "Block A — Saturday".
2. Admin assigns orders to batches and assigns delivery staff.
3. Delivery staff sees their batch via `GET /api/batches/{id}/orders`.
4. Staff marks each order delivered → `PUT /api/batches/{id}/orders/{orderId}/delivered`.
5. `customer_order.status` updated to `DELIVERED`.
6. Push notification sent to customer on delivery.

---

## 11. notification + fcm_token

**Purpose:** In-app and push notifications for order updates and announcements.

**Flow:**
1. Customer registers FCM token on app launch → `POST /api/fcm/register`.
2. On key events (order confirmed, transport stage, delivery) → `NotificationService` creates `notification` row + sends FCM push via Firebase.
3. Broadcast notifications (announcements, offers) → sent to all active FCM tokens.
4. Customer reads notifications in-app → `PUT /api/notifications/{id}/read`.

---

## 12. referral

**Purpose:** Track referrals for community growth.

**Flow:**
1. Each user gets a unique `referral_code` on registration.
2. New user applies code at signup → `POST /api/referrals/apply`.
3. `referral` row created linking referrer → referred.
4. Status = `PENDING` until reward logic is implemented (Phase 2).

---

## 13. master_table

**Purpose:** Centralised lookup/dropdown values.

**Flow:**
- Seeded at DB setup.
- Frontend calls `GET /api/masters/{type}` to populate dropdowns.
- Admin can add/update entries.

---

## 14. invalidated_token

**Purpose:** JWT logout blacklist.

**Flow:**
1. On `POST /api/auth/logout`, token saved here.
2. `JwtFilter` checks this table on every request.
3. Scheduled cleanup deletes expired tokens daily.
