# Screens, Routes & API Mapping

Every screen listed with its route, access role, APIs called on load, and APIs called on user action.

---

## Auth Screens

### `/auth/login` — Login
**Access:** Public (redirect to home if already logged in)
**On Load:** Nothing
**On Submit (Send OTP):**
- `POST /api/auth/send-otp` → `{ phoneNumber }`
- On success → navigate to `/auth/verify` with phone in router state

---

### `/auth/verify` — Verify OTP
**Access:** Public
**On Load:** Nothing (phone number passed via router state)
**On Submit (Verify OTP):**
- `POST /api/auth/verify-otp` → `{ phoneNumber, otp, referralCode? }`
- On success → store JWT → role-based redirect
**On Click (Resend OTP):**
- `POST /api/auth/send-otp` → same as login submit
- 60-second cooldown before resend is enabled

---

## Customer Screens

### `/customer/home` — Home
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/cycles/current` → show cycle status + countdown timer
- `GET /api/notifications/my` → unread count for bell badge
**Countdown Timer:** Uses `timeRemainingSeconds` from cycle response, counts down live
**Cycle Status Display:**
- `OPEN` → "Ordering open! Closes in X hours" + Shop Now button
- `CLOSED` → "Ordering closed. Procurement in progress."
- `PROCUREMENT` → "We're buying your groceries in Kadapa!"
- `DELIVERING` → "Your order is on the way!"
- `COMPLETED` → "Delivered! Next cycle opens Monday."
- `null` → "Next ordering window opens Monday."

---

### `/customer/products` — Browse Products
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/categories` → populate category filter tabs
- `GET /api/products` → load all available products
**On Category Tab Click:**
- `GET /api/products?category={id}`
**On Search Input:**
- `GET /api/products?search={query}` (debounced 300ms)
**On Add to Cart:** Local state only — no API call
**On Product Card Click:** Navigate to `/customer/products/{id}`

---

### `/customer/products/:id` — Product Detail
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/products/{id}`
**On Add to Cart:** Local state only

---

### `/customer/cart` — Cart
**Access:** ROLE_CUSTOMER
**On Load:** Read from local cart state (no API)
**On Load (for referral discount display):**
- `GET /api/referrals/my` → check if any PENDING referral discount applies
**On Checkout Click:** Navigate to `/customer/checkout`

---

### `/customer/checkout` — Checkout
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/cycles/current` → validate cycle is still OPEN before showing form
- `GET /api/masters/DELIVERY_SLOT` → populate delivery slot dropdown (SAT/SUN)
**On Place Order:**
- `POST /api/orders` → `{ items: [{productId, quantity}], notes }`
- On success → clear cart → navigate to `/customer/orders/{newOrderId}`
- On error (cycle closed) → show "Ordering has closed" message

---

### `/customer/orders` — My Orders
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/orders/my`
**On Order Row Click:** Navigate to `/customer/orders/{id}`

---

### `/customer/orders/:id` — Order Detail
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/orders/my/{id}`
- `GET /api/transport/{cycleId}` → show transport tracking timeline
**On Cancel Click:**
- `PUT /api/orders/{id}/cancel`
- Only show Cancel button if order status is `PLACED` and cycle is `OPEN`

---

### `/customer/notifications` — Notifications
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/notifications/my`
**On Notification Click:**
- `PUT /api/notifications/{id}/read`
**On "Mark All Read" Click:**
- `PUT /api/notifications/read-all`

---

### `/customer/profile` — Profile
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/users/me`
**On Save:**
- `PUT /api/users/me` → `{ username, firstName, lastName, email, flatNumber, block }`

---

### `/customer/referral` — Referral
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/referrals/my-code` → show referral code + share button
- `GET /api/referrals/my` → show referral history list

---

### `/customer/feedback` — Feedback Wall
**Access:** ROLE_CUSTOMER
**On Load:**
- `GET /api/feedback`
**On Post Feedback:**
- `POST /api/feedback` → `{ rating, message }`
**On Helpful Click:**
- `POST /api/feedback/{id}/helpful`

---

## Admin Screens

### `/admin/dashboard` — Dashboard
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/dashboard/summary`
- `GET /api/cycles/current`

---

### `/admin/cycles` — Weekly Cycle Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/cycles`
**On Open Click:**
- `PUT /api/cycles/{id}/open`
**On Close Click:**
- `PUT /api/cycles/{id}/close`
**On Status Change:**
- `PUT /api/cycles/{id}/status` → `{ status }`
**On Create Cycle:**
- `POST /api/cycles`

---

### `/admin/products` — Product Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/products`
- `GET /api/categories` → for category filter + create form dropdown
**On Create:**
- `POST /api/products`
**On Edit:**
- `PUT /api/products/{id}`
**On Toggle Availability:**
- `PUT /api/products/{id}/availability` → `{ available: true/false }`

---

### `/admin/categories` — Category Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/categories`
**On Create:**
- `POST /api/categories`
**On Edit:**
- `PUT /api/categories/{id}`

---

### `/admin/orders` — All Orders
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/cycles` → cycle filter dropdown
- `GET /api/orders?cycleId={id}` → orders for selected cycle
**On Status Change:**
- `PUT /api/orders/{id}/status` → `{ status }`
**On Row Click:** Navigate to `/admin/orders/{id}`

---

### `/admin/orders/:id` — Order Detail
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/orders/{id}`

---

### `/admin/procurement/:cycleId` — Procurement Sheet
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/procurement/{cycleId}`
**On Inline Edit (vendor/qty):**
- `PUT /api/procurement/{cycleId}/{productId}`
**On Export Click:**
- `GET /api/procurement/{cycleId}/export` → download `.xlsx` file

---

### `/admin/transport/:cycleId` — Transport Tracking
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/transport/{cycleId}` → show stage timeline
**On Add Stage:**
- `POST /api/transport/{cycleId}/stage` → `{ stage, notes }`

---

### `/admin/batches/:cycleId` — Delivery Batches
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/batches/{cycleId}`
- `GET /api/users?role=ROLE_DELIVERY` → delivery staff dropdown for assignment
**On Create Batch:**
- `POST /api/batches`
**On Assign Staff:**
- `PUT /api/batches/{id}/assign` → `{ assignedTo: userId }`
**On Batch Row Click:** Navigate to `/admin/batches/{id}/orders`

---

### `/admin/batches/:id/orders` — Batch Orders (Delivery View)
**Access:** ROLE_ADMIN, ROLE_DELIVERY
**On Load:**
- `GET /api/batches/{id}/orders`
**On Mark Delivered:**
- `PUT /api/batches/{id}/orders/{orderId}/delivered`

---

### `/admin/users` — User Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/users`
**On Block:**
- `PUT /api/users/{id}/block`
**On Unblock:**
- `PUT /api/users/{id}/unblock`
**On Assign Role:**
- `POST /api/users/{id}/roles` → `{ roleId }`

---

### `/admin/notifications` — Send Notifications
**Access:** ROLE_ADMIN
**On Broadcast:**
- `POST /api/notifications/broadcast`
**On Send to User:**
- `POST /api/notifications/send`

---

### `/admin/feedback` — Feedback Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/feedback/admin`
**On Reply:**
- `POST /api/feedback/{id}/reply`
**On Pin:**
- `PUT /api/feedback/{id}/pin`
**On Flag:**
- `PUT /api/feedback/{id}/flag`

---

### `/admin/masters` — Master Table Management
**Access:** ROLE_ADMIN
**On Load:**
- `GET /api/masters/{type}` for each type
**On Create:**
- `POST /api/masters`
**On Edit:**
- `PUT /api/masters/{id}`

---

### `/admin/communities` — Community Management
**Access:** ROLE_SUPER_ADMIN
**On Load:**
- `GET /api/communities`
**On Create:**
- `POST /api/communities`
**On Edit:**
- `PUT /api/communities/{id}`

---

## Route Summary Table

| Route | Module | Role | Primary API |
|-------|--------|------|-------------|
| `/auth/login` | Auth | Public | `POST /auth/send-otp` |
| `/auth/verify` | Auth | Public | `POST /auth/verify-otp` |
| `/customer/home` | Customer | CUSTOMER | `GET /cycles/current` |
| `/customer/products` | Customer | CUSTOMER | `GET /products` |
| `/customer/products/:id` | Customer | CUSTOMER | `GET /products/:id` |
| `/customer/cart` | Customer | CUSTOMER | Local state |
| `/customer/checkout` | Customer | CUSTOMER | `POST /orders` |
| `/customer/orders` | Customer | CUSTOMER | `GET /orders/my` |
| `/customer/orders/:id` | Customer | CUSTOMER | `GET /orders/my/:id` |
| `/customer/notifications` | Customer | CUSTOMER | `GET /notifications/my` |
| `/customer/profile` | Customer | CUSTOMER | `GET /users/me` |
| `/customer/referral` | Customer | CUSTOMER | `GET /referrals/my-code` |
| `/customer/feedback` | Customer | CUSTOMER | `GET /feedback` |
| `/admin/dashboard` | Admin | ADMIN | `GET /dashboard/summary` |
| `/admin/cycles` | Admin | ADMIN | `GET /cycles` |
| `/admin/products` | Admin | ADMIN | `GET /products` |
| `/admin/categories` | Admin | ADMIN | `GET /categories` |
| `/admin/orders` | Admin | ADMIN | `GET /orders` |
| `/admin/orders/:id` | Admin | ADMIN | `GET /orders/:id` |
| `/admin/procurement/:cycleId` | Admin | ADMIN | `GET /procurement/:cycleId` |
| `/admin/transport/:cycleId` | Admin | ADMIN | `GET /transport/:cycleId` |
| `/admin/batches/:cycleId` | Admin | ADMIN | `GET /batches/:cycleId` |
| `/admin/batches/:id/orders` | Admin | ADMIN,DELIVERY | `GET /batches/:id/orders` |
| `/admin/users` | Admin | ADMIN | `GET /users` |
| `/admin/notifications` | Admin | ADMIN | `POST /notifications/broadcast` |
| `/admin/feedback` | Admin | ADMIN | `GET /feedback/admin` |
| `/admin/masters` | Admin | ADMIN | `GET /masters/:type` |
| `/admin/communities` | Admin | SUPER_ADMIN | `GET /communities` |
