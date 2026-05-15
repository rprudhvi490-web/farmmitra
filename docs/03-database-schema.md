# Database Schema — WeekendBasketDB

All tables include audit columns unless noted:
`created_by VARCHAR(100)`, `created_on TIMESTAMP`, `updated_on TIMESTAMP`, `updated_by VARCHAR(100)`

**PostgreSQL type reference:**

| Type | Meaning |
|------|---------|
| `BIGSERIAL` | Auto-increment BIGINT — use for all PK columns (replaces Oracle NUMBER PK) |
| `BIGINT` | 64-bit integer — use for all FK columns |
| `VARCHAR(n)` | Unicode string — PostgreSQL VARCHAR is UTF-8 natively, no NVARCHAR2 needed |
| `TEXT` | Unlimited length string (replaces Oracle CLOB) |
| `DECIMAL(p,s)` | Exact decimal (same as Oracle NUMBER(p,s)) |
| `BOOLEAN` | true / false |
| `DATE` | Date only |
| `TIMESTAMP` | Date + time |

---

## app_user

| Column        | Type         | Notes                                               |
|---------------|--------------|-----------------------------------------------------|
| id            | BIGSERIAL PK |                                                     |
| phone_number  | VARCHAR(15)  | UNIQUE, NOT NULL — primary identity                 |
| password      | VARCHAR(255) | BCrypt(phone_number) — never shown in UI            |
| username      | VARCHAR(100) | Display name, default = "guest" until user updates  |
| referral_code | VARCHAR(20)  | UNIQUE — auto-generated on registration             |
| status        | VARCHAR(20)  | ACTIVE / BLOCKED                                    |
| + audit cols  |              |                                                     |

**Notes:**
- `password` = BCrypt hash of phone number. Enables identical Spring Security pattern to ElderCare.
- `username` is display only — NOT unique. `phone_number` is the unique identity always.
- `status = BLOCKED` → JwtFilter rejects all requests even with a valid JWT token.
- `referral_code` stored here for quick lookup when a new user applies a referral code.

---

## user_profile

| Column       | Type         | Notes                                         |
|--------------|--------------|-----------------------------------------------|
| id           | BIGSERIAL PK |                                               |
| first_name   | VARCHAR(100) |                                               |
| last_name    | VARCHAR(100) |                                               |
| email        | VARCHAR(150) |                                               |
| flat_number  | VARCHAR(50)  | e.g. A-204                                    |
| block        | VARCHAR(50)  | e.g. Block A                                  |
| community_id | BIGINT       | Reserved for future multi-community expansion |
| user_id      | BIGINT FK    | → app_user.id                                 |
| + audit cols |              |                                               |

**Notes:**
- Auto-created (empty) when user first verifies OTP.
- User fills it via `PUT /api/users/me`.
- `community_id` not used in MVP — single community only. Kept for future expansion.

---

## community

| Column       | Type         | Notes                     |
|--------------|--------------|---------------------------|
| id           | BIGSERIAL PK |                           |
| name         | VARCHAR(200) | e.g. Prestige Green Woods |
| city         | VARCHAR(100) |                           |
| address      | TEXT         |                           |
| active       | BOOLEAN      | default true              |
| + audit cols |              |                           |

**Notes:** Single community for MVP. Table exists for future multi-community expansion only.

---

## role

| Column       | Type         | Notes                     |
|--------------|--------------|---------------------------|
| id           | BIGSERIAL PK |                           |
| role_name    | VARCHAR(100) | e.g. Customer             |
| role_id      | VARCHAR(50)  | e.g. ROLE_CUSTOMER UNIQUE |
| + audit cols |              |                           |

**Seed data:**

| id | role_name   | role_id          |
|----|-------------|------------------|
| 1  | Super Admin | ROLE_SUPER_ADMIN |
| 2  | Admin       | ROLE_ADMIN       |
| 3  | Customer    | ROLE_CUSTOMER    |
| 4  | Delivery    | ROLE_DELIVERY    |

---

## role_access

| Column       | Type         | Notes         |
|--------------|--------------|---------------|
| id           | BIGSERIAL PK |               |
| role_id      | BIGINT FK    | → role.id     |
| user_id      | BIGINT FK    | → app_user.id |
| + audit cols |              |               |

---

## otp_verification

| Column       | Type         | Notes                          |
|--------------|--------------|--------------------------------|
| id           | BIGSERIAL PK |                                |
| phone_number | VARCHAR(15)  |                                |
| otp_code     | VARCHAR(10)  | Plain 6-digit OTP              |
| expires_at   | TIMESTAMP    | created_on + 5 minutes         |
| attempts     | INT          | default 0, max 3               |
| verified     | BOOLEAN      | default false                  |
| created_on   | TIMESTAMP    |                                |

> No audit columns — transient record. Cleaned up daily by scheduler.

---

## invalidated_token

| Column         | Type         | Notes                                                              |
|----------------|--------------|--------------------------------------------------------------------|
| id             | BIGSERIAL PK |                                                                    |
| token          | TEXT         |                                                                    |
| invalidated_at | TIMESTAMP    |                                                                    |

**Notes:** Checked in JwtFilter on every request. Multiple device tokens are independent —
logout on one device does not affect other devices.

---

## category

| Column        | Type         | Notes                         |
|---------------|--------------|-------------------------------|
| id            | BIGSERIAL PK |                               |
| name          | VARCHAR(100) | e.g. Vegetables, Fruits, Rice |
| image_url     | VARCHAR(500) | Cloudinary URL                |
| display_order | INT          | Sort order in app             |
| active        | BOOLEAN      | default true                  |
| + audit cols  |              |                               |

---

## product

| Column         | Type          | Notes                      |
|----------------|---------------|----------------------------|
| id             | BIGSERIAL PK  |                            |
| name           | VARCHAR(200)  |                            |
| description    | TEXT          |                            |
| category_id    | BIGINT FK     | → category.id              |
| unit           | VARCHAR(50)   | e.g. kg, piece, litre      |
| price_per_unit | DECIMAL(10,2) |                            |
| image_url      | VARCHAR(500)  | Cloudinary URL             |
| available      | BOOLEAN       | true = available this week |
| min_order_qty  | DECIMAL(6,2)  | Minimum order quantity     |
| + audit cols   |               |                            |

---

## weekly_cycle

| Column            | Type          | Notes                                                |
|-------------------|---------------|------------------------------------------------------|
| id                | BIGSERIAL PK  |                                                      |
| cycle_label       | VARCHAR(100)  | e.g. "Week of 12 Jan 2025"                           |
| order_open_at     | TIMESTAMP     | Monday — ordering opens                              |
| order_close_at    | TIMESTAMP     | Wednesday — ordering closes                          |
| delivery_date_sat | DATE          | Saturday delivery date                               |
| delivery_date_sun | DATE          | Sunday delivery date                                 |
| status            | VARCHAR(30)   | OPEN / CLOSED / PROCUREMENT / DELIVERING / COMPLETED |
| + audit cols      |               |                                                      |

**Notes:**
- Scheduler auto-creates every Monday, auto-closes every Wednesday 14:00.
- Admin can force open/close via API at any time.
- `timeRemainingSeconds` is computed at query time — not stored in DB.

---

## customer_order

| Column            | Type          | Notes                                               |
|-------------------|---------------|-----------------------------------------------------|
| id                | BIGSERIAL PK  |                                                     |
| order_number      | VARCHAR(50)   | UNIQUE, system-generated e.g. WB-2025-0001          |
| user_id           | BIGINT FK     | → app_user.id                                       |
| cycle_id          | BIGINT FK     | → weekly_cycle.id                                   |
| delivery_slot     | VARCHAR(10)   | SAT / SUN — assigned by admin after route algorithm |
| status            | VARCHAR(30)   | PLACED / CONFIRMED / PACKED / DELIVERED / CANCELLED |
| total_amount      | DECIMAL(10,2) |                                                     |
| referral_discount | DECIMAL(10,2) | default 0 — ₹100 per successful referral            |
| amount_to_collect | DECIMAL(10,2) | total_amount - referral_discount                    |
| payment_method    | VARCHAR(30)   | COD only for MVP                                    |
| payment_status    | VARCHAR(20)   | PENDING / PAID                                      |
| notes             | TEXT          | Customer delivery notes                             |
| + audit cols      |               |                                                     |

**Notes:**
- `delivery_slot` is NULL at order placement. Admin assigns SAT/SUN after delivery route algorithm runs.
- `referral_discount` = ₹100 when referrer's referred user places their first order.
- `amount_to_collect` shown to delivery staff — this is what they physically collect at the door.

---

## order_item

| Column       | Type          | Notes                  |
|--------------|---------------|------------------------|
| id           | BIGSERIAL PK  |                        |
| order_id     | BIGINT FK     | → customer_order.id    |
| product_id   | BIGINT FK     | → product.id           |
| quantity     | DECIMAL(6,2)  |                        |
| unit_price   | DECIMAL(10,2) | Price at time of order |
| total_price  | DECIMAL(10,2) | quantity × unit_price  |
| + audit cols |               |                        |

---

## procurement_sheet

| Column         | Type          | Notes                      |
|----------------|---------------|----------------------------|
| id             | BIGSERIAL PK  |                            |
| cycle_id       | BIGINT FK     | → weekly_cycle.id          |
| product_id     | BIGINT FK     | → product.id               |
| total_quantity | DECIMAL(8,2)  | Aggregated from all orders |
| unit           | VARCHAR(50)   |                            |
| vendor_name    | VARCHAR(200)  | Supplier/market name       |
| vendor_notes   | TEXT          |                            |
| procured_qty   | DECIMAL(8,2)  | Actual quantity procured   |
| status         | VARCHAR(30)   | PENDING / PROCURED         |
| + audit cols   |               |                            |

**Notes:** Auto-populated when cycle closes via aggregation query. Admin fills vendor info and procured qty.

---

## transport_tracking

| Column     | Type         | Notes                                                                             |
|------------|--------------|-----------------------------------------------------------------------------------|
| id         | BIGSERIAL PK |                                                                                   |
| cycle_id   | BIGINT FK    | → weekly_cycle.id                                                                 |
| stage      | VARCHAR(50)  | PROCUREMENT_STARTED / GOODS_LOADED / IN_TRANSIT / ARRIVED / PACKING / DISPATCHED |
| notes      | TEXT         |                                                                                   |
| updated_by | VARCHAR(100) |                                                                                   |
| created_on | TIMESTAMP    |                                                                                   |

> Append-only log. No audit columns — this IS the tracking log.
> Each stage update triggers push notification to all customers with orders in that cycle.

---

## delivery_batch

| Column        | Type         | Notes                          |
|---------------|--------------|--------------------------------|
| id            | BIGSERIAL PK |                                |
| cycle_id      | BIGINT FK    | → weekly_cycle.id              |
| batch_label   | VARCHAR(100) | e.g. "Block A — Saturday"      |
| delivery_date | DATE         |                                |
| assigned_to   | BIGINT FK    | → app_user.id (delivery staff) |
| status        | VARCHAR(20)  | PENDING / IN_PROGRESS / DONE   |
| + audit cols  |              |                                |

**Notes:** Batches created by admin after delivery route algorithm runs. Groups orders by block/building proximity.
See `13-delivery-algorithm.md`.

---

## delivery_batch_order

| Column   | Type         | Notes               |
|----------|--------------|---------------------|
| id       | BIGSERIAL PK |                     |
| batch_id | BIGINT FK    | → delivery_batch.id |
| order_id | BIGINT FK    | → customer_order.id |

---

## notification

| Column      | Type         | Notes                                          |
|-------------|--------------|------------------------------------------------|
| id          | BIGSERIAL PK |                                                |
| user_id     | BIGINT FK    | → app_user.id — NULL means broadcast to all    |
| title       | VARCHAR(200) |                                                |
| body        | TEXT         |                                                |
| type        | VARCHAR(50)  | ORDER_UPDATE / ANNOUNCEMENT / OFFER / REMINDER |
| read_status | BOOLEAN      | default false                                  |
| sent_at     | TIMESTAMP    |                                                |
| + audit cols|              |                                                |

---

## fcm_token

| Column      | Type         | Notes                                                              |
|-------------|--------------|--------------------------------------------------------------------|
| id          | BIGSERIAL PK |                                                                    |
| user_id     | BIGINT FK    | → app_user.id                                                      |
| token       | TEXT         | FCM device token                                                   |
| device_type | VARCHAR(20)  | ANDROID / IOS                                                      |
| active      | BOOLEAN      | default true                                                       |
| + audit cols|              |                                                                    |

**Notes:** One user can have multiple rows (multiple devices). All active tokens receive push.
Stale tokens marked `active = false` when FCM returns UNREGISTERED error.

---

## referral

| Column        | Type         | Notes                                                    |
|---------------|--------------|----------------------------------------------------------|
| id            | BIGSERIAL PK |                                                          |
| referrer_id   | BIGINT FK    | → app_user.id (who referred)                             |
| referred_id   | BIGINT FK    | → app_user.id (who joined)                               |
| referral_code | VARCHAR(20)  | Code that was used — copied from app_user.referral_code  |
| status        | VARCHAR(20)  | PENDING / REWARDED                                       |
| + audit cols  |              |                                                          |

**Notes:**
- `status = REWARDED` when referred user's first order is DELIVERED.
- On REWARDED: ₹100 `referral_discount` applied to referrer's next order automatically.

---

## master_table

| Column       | Type         | Notes                                |
|--------------|--------------|--------------------------------------|
| id           | BIGSERIAL PK |                                      |
| lookup_value | VARCHAR(10)  | Short key / sort order               |
| lookup_item  | VARCHAR(100) | Display label (e.g. Saturday)        |
| lookup_code  | VARCHAR(50)  | Code used in backend logic           |
| type         | VARCHAR(50)  | ORDER_STATUS / PAYMENT_METHOD / etc. |
| + audit cols |              |                                      |

Master types:
1. ORDER_STATUS
2. PAYMENT_METHOD
3. PAYMENT_STATUS
4. DELIVERY_SLOT
5. CYCLE_STATUS
6. TRANSPORT_STAGE
7. NOTIFICATION_TYPE

---

## Tables Count Summary

| #  | Table                | Purpose                          |
|----|----------------------|----------------------------------|
| 1  | app_user             | Phone-based identity + BCrypt    |
| 2  | user_profile         | Display name, flat, block        |
| 3  | community            | Future multi-community support   |
| 4  | role                 | Role catalogue (4 roles)         |
| 5  | role_access          | User → role mapping              |
| 6  | otp_verification     | OTP storage + expiry             |
| 7  | invalidated_token    | JWT logout blacklist             |
| 8  | category             | Product categories               |
| 9  | product              | Product catalogue                |
| 10 | weekly_cycle         | Weekly ordering window           |
| 11 | customer_order       | Customer orders + COD amounts    |
| 12 | order_item           | Line items per order             |
| 13 | procurement_sheet    | Aggregated procurement per cycle |
| 14 | transport_tracking   | Transport stage log              |
| 15 | delivery_batch       | Delivery grouping by block       |
| 16 | delivery_batch_order | Order → batch mapping            |
| 17 | notification         | In-app notifications             |
| 18 | fcm_token            | FCM device tokens per user       |
| 19 | referral             | Referral tracking + ₹100 reward  |
| 20 | master_table         | Lookup/dropdown values           |
