# API Inventory

Base path: `/api`

All protected endpoints require `Authorization: Bearer <token>` header.

---

## Auth

| Method | Path                    | Access  | Description                        |
|--------|-------------------------|---------|------------------------------------|
| POST   | /auth/send-otp          | Public  | Send OTP to phone number           |
| POST   | /auth/verify-otp        | Public  | Verify OTP, returns JWT            |
| POST   | /auth/logout            | Any auth| Invalidate token                   |
| GET    | /health                 | Public  | Health check                       |
| GET    | /actuator/**            | Public (dev) / ADMIN (prod) | Actuator |

---

## User & Profile

| Method | Path                  | Role         | Description                    |
|--------|-----------------------|--------------|--------------------------------|
| GET    | /users/me             | Any auth     | Get own profile                |
| PUT    | /users/me             | Any auth     | Update own profile             |
| GET    | /users                | ADMIN        | List all users                 |
| GET    | /users/{id}           | ADMIN        | Get user by id                 |
| PUT    | /users/{id}/block     | ADMIN        | Block a user                   |
| PUT    | /users/{id}/unblock   | ADMIN        | Unblock a user                 |
| POST   | /users/{id}/roles     | ADMIN        | Assign role to user            |

---

## Role Management

| Method | Path          | Role  | Description                        |
|--------|---------------|-------|------------------------------------|
| GET    | /roles        | ADMIN | List all roles                     |
| POST   | /roles        | ADMIN | Create role (ROLE_ auto-prepended) |

---

## Community

| Method | Path              | Role        | Description              |
|--------|-------------------|-------------|--------------------------|
| GET    | /communities      | ADMIN       | List communities         |
| POST   | /communities      | SUPER_ADMIN | Create community         |
| PUT    | /communities/{id} | SUPER_ADMIN | Update community         |

---

## Categories

| Method | Path                | Role     | Description              |
|--------|---------------------|----------|--------------------------|
| GET    | /categories         | Any auth | List active categories   |
| POST   | /categories         | ADMIN    | Create category          |
| PUT    | /categories/{id}    | ADMIN    | Update category          |

---

## Products

| Method | Path                       | Role     | Description                    |
|--------|----------------------------|----------|--------------------------------|
| GET    | /products                  | Any auth | List available products        |
| GET    | /products?category={id}    | Any auth | Filter by category             |
| GET    | /products?search={q}       | Any auth | Search products                |
| GET    | /products/{id}             | Any auth | Get product detail             |
| POST   | /products                  | ADMIN    | Create product                 |
| PUT    | /products/{id}             | ADMIN    | Update product                 |
| PUT    | /products/{id}/availability| ADMIN    | Toggle weekly availability     |

---

## Weekly Cycle

| Method | Path                    | Role     | Description                        |
|--------|-------------------------|----------|------------------------------------|
| GET    | /cycles/current         | Any auth | Get current active cycle           |
| GET    | /cycles                 | ADMIN    | List all cycles                    |
| POST   | /cycles                 | ADMIN    | Create new cycle manually          |
| PUT    | /cycles/{id}/open       | ADMIN    | Force open ordering                |
| PUT    | /cycles/{id}/close      | ADMIN    | Force close ordering               |
| PUT    | /cycles/{id}/status     | ADMIN    | Update cycle status                |

---

## Orders (Customer)

| Method | Path                        | Role     | Description                    |
|--------|-----------------------------|----------|--------------------------------|
| POST   | /orders                     | CUSTOMER | Place order                    |
| GET    | /orders/my                  | CUSTOMER | My order history               |
| GET    | /orders/my/{id}             | CUSTOMER | My order detail                |
| PUT    | /orders/{id}/cancel         | CUSTOMER | Cancel order (if cycle open)   |

---

## Orders (Admin)

| Method | Path                        | Role  | Description                        |
|--------|-----------------------------|-------|------------------------------------|
| GET    | /orders                     | ADMIN | List all orders (filter by cycle)  |
| GET    | /orders/{id}                | ADMIN | Get order detail                   |
| PUT    | /orders/{id}/status         | ADMIN | Update order status                |
| GET    | /orders/cycle/{cycleId}     | ADMIN | All orders for a cycle             |

---

## Procurement

| Method | Path                                    | Role  | Description                        |
|--------|-----------------------------------------|-------|------------------------------------|
| GET    | /procurement/{cycleId}                  | ADMIN | Get aggregated procurement sheet   |
| PUT    | /procurement/{cycleId}/{productId}      | ADMIN | Update vendor / procured qty       |
| GET    | /procurement/{cycleId}/export           | ADMIN | Export procurement sheet (Excel)   |

---

## Transport Tracking

| Method | Path                              | Role  | Description                    |
|--------|-----------------------------------|-------|--------------------------------|
| GET    | /transport/{cycleId}              | ADMIN | Get transport stage log        |
| POST   | /transport/{cycleId}/stage        | ADMIN | Add transport stage update     |

---

## Delivery Batches

| Method | Path                                    | Role     | Description                    |
|--------|-----------------------------------------|----------|--------------------------------|
| GET    | /batches/{cycleId}                      | ADMIN    | List batches for a cycle       |
| POST   | /batches                                | ADMIN    | Create delivery batch          |
| PUT    | /batches/{id}/assign                    | ADMIN    | Assign delivery staff          |
| GET    | /batches/{id}/orders                    | ADMIN,DELIVERY | Orders in a batch        |
| PUT    | /batches/{id}/orders/{orderId}/delivered| DELIVERY | Mark order as delivered       |

---

## Notifications

| Method | Path                          | Role     | Description                    |
|--------|-------------------------------|----------|--------------------------------|
| GET    | /notifications/my             | Any auth | My notifications               |
| PUT    | /notifications/{id}/read      | Any auth | Mark as read                   |
| PUT    | /notifications/read-all       | Any auth | Mark all as read               |
| POST   | /notifications/broadcast      | ADMIN    | Send broadcast notification    |
| POST   | /notifications/send           | ADMIN    | Send to specific user          |

---

## FCM Tokens

| Method | Path                  | Role     | Description                    |
|--------|-----------------------|----------|--------------------------------|
| POST   | /fcm/register         | Any auth | Register device FCM token      |
| DELETE | /fcm/{tokenId}        | Any auth | Remove FCM token               |

---

## Referrals

| Method | Path                  | Role     | Description                    |
|--------|-----------------------|----------|--------------------------------|
| GET    | /referrals/my-code    | CUSTOMER | Get my referral code           |
| GET    | /referrals/my         | CUSTOMER | My referral history            |
| POST   | /referrals/apply      | CUSTOMER | Apply referral code at signup  |

---

## Masters

| Method | Path                  | Role     | Description                    |
|--------|-----------------------|----------|--------------------------------|
| GET    | /masters/{type}       | Any auth | Get lookup list by type        |
| POST   | /masters              | ADMIN    | Add master entry               |
| PUT    | /masters/{id}         | ADMIN    | Update master entry            |

---

## Admin Dashboard

| Method | Path                          | Role  | Description                        |
|--------|-------------------------------|-------|------------------------------------|
| GET    | /dashboard/summary            | ADMIN | Weekly stats — orders, revenue     |
| GET    | /dashboard/cycle/{cycleId}    | ADMIN | Cycle-specific stats               |
