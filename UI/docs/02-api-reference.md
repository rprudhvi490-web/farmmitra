# API Reference — All Backend Endpoints

Base URL (dev): `http://localhost:9090/weekendbasket/api`
Base URL (prod): `https://<render-url>/weekendbasket/api`

All protected endpoints require header: `Authorization: Bearer <jwt_token>`

Error response shape (all errors):
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "OTP has expired",
  "timestamp": "2025-01-01T10:00:00"
}
```

---

## 1. Auth

### POST `/auth/send-otp`
**Access:** Public
**When to call:** User submits phone number on login screen

Request:
```json
{ "phoneNumber": "9876543210" }
```
Response:
```json
{ "message": "OTP sent successfully", "phoneNumber": "9876543210" }
```
Errors: `400` invalid phone format

---

### POST `/auth/verify-otp`
**Access:** Public
**When to call:** User submits 6-digit OTP

Request:
```json
{
  "phoneNumber": "9876543210",
  "otp": "123456",
  "referralCode": "ABC12345"
}
```
`referralCode` is optional — only on first-time signup.

Response:
```json
{
  "token": "<jwt>",
  "expiresIn": 36000000,
  "phoneNumber": "9876543210",
  "username": "guest",
  "roles": ["ROLE_CUSTOMER"],
  "isNewUser": true,
  "hasPassword": false
}
```
Angular action after success:
- Store `token` in localStorage
- Decode token to get roles
- If `isNewUser = true` → redirect to profile completion
- If `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` → redirect to `/admin/dashboard`
- If `ROLE_CUSTOMER` → redirect to `/customer/home`

Errors: `400` wrong OTP, `429` max attempts exceeded

---

### POST `/auth/logout`
**Access:** Any authenticated
**When to call:** User clicks logout

Request: No body — JWT in header is invalidated server-side.

Response: `200 OK`

Angular action: Clear localStorage, redirect to `/auth/login`

---

### GET `/health`
**Access:** Public
**When to call:** App startup health check (optional)

Response: `{ "status": "UP" }`

---

## 2. User & Profile

### GET `/users/me`
**Access:** Any authenticated
**When to call:** Profile page load, after login to show username

Response:
```json
{
  "id": 1,
  "phoneNumber": "9876543210",
  "username": "Ravi Kumar",
  "status": "ACTIVE",
  "hasPassword": false,
  "profile": {
    "firstName": "Ravi",
    "lastName": "Kumar",
    "email": "ravi@example.com",
    "flatNumber": "A-204",
    "block": "Block A",
    "communityId": null
  }
}
```

---

### PUT `/users/me`
**Access:** Any authenticated
**When to call:** User saves profile form

Request:
```json
{
  "username": "Ravi Kumar",
  "firstName": "Ravi",
  "lastName": "Kumar",
  "email": "ravi@example.com",
  "flatNumber": "A-204",
  "block": "Block A"
}
```
Response: Updated user object (same shape as GET `/users/me`)

---

### GET `/users`
**Access:** ADMIN
**When to call:** Admin users list page load

Response: Array of user objects

---

### GET `/users/{id}`
**Access:** ADMIN
**When to call:** Admin clicks on a user row

---

### PUT `/users/{id}/block`
**Access:** ADMIN
**When to call:** Admin clicks Block button on user

Request: No body
Response: `200 OK`

---

### PUT `/users/{id}/unblock`
**Access:** ADMIN
**When to call:** Admin clicks Unblock button on user

---

### POST `/users/{id}/roles`
**Access:** ADMIN
**When to call:** Admin assigns a role to a user

Request:
```json
{ "roleId": "ROLE_DELIVERY" }
```

---

## 3. Roles

### GET `/roles`
**Access:** ADMIN
**When to call:** Role management page, role assignment dropdown

Response:
```json
[
  { "id": 1, "roleName": "Super Admin", "roleId": "ROLE_SUPER_ADMIN" },
  { "id": 2, "roleName": "Admin", "roleId": "ROLE_ADMIN" },
  { "id": 3, "roleName": "Customer", "roleId": "ROLE_CUSTOMER" },
  { "id": 4, "roleName": "Delivery", "roleId": "ROLE_DELIVERY" }
]
```

---

### POST `/roles`
**Access:** ADMIN
**When to call:** Admin creates a new role

Request:
```json
{ "roleName": "Vendor", "roleId": "VENDOR" }
```
Note: Backend auto-prepends `ROLE_` → stored as `ROLE_VENDOR`

---

## 4. Community

### GET `/communities`
**Access:** ADMIN
**When to call:** Community management page

---

### POST `/communities`
**Access:** SUPER_ADMIN
**When to call:** Create new community form submit

Request:
```json
{
  "name": "Prestige Green Woods",
  "city": "Hyderabad",
  "address": "Nanakramguda, Hyderabad"
}
```

---

### PUT `/communities/{id}`
**Access:** SUPER_ADMIN
**When to call:** Edit community form submit

---

## 5. Categories

### GET `/categories`
**Access:** Any authenticated
**When to call:** Product browse page load, category filter dropdown

Response:
```json
[
  { "id": 1, "name": "Vegetables", "imageUrl": "https://...", "displayOrder": 1, "active": true },
  { "id": 2, "name": "Fruits", "imageUrl": "https://...", "displayOrder": 2, "active": true }
]
```

---

### POST `/categories`
**Access:** ADMIN
**When to call:** Admin creates category

Request:
```json
{ "name": "Vegetables", "imageUrl": "https://cloudinary.com/...", "displayOrder": 1 }
```

---

### PUT `/categories/{id}`
**Access:** ADMIN
**When to call:** Admin edits category

---

## 6. Products

### GET `/products`
**Access:** Any authenticated
**When to call:** Customer product browse page, admin product list

Query params:
- `?category={id}` — filter by category
- `?search={q}` — search by name

Response:
```json
[
  {
    "id": 1,
    "name": "Tomatoes",
    "description": "Fresh farm tomatoes",
    "categoryId": 1,
    "categoryName": "Vegetables",
    "unit": "kg",
    "pricePerUnit": 40.00,
    "imageUrl": "https://...",
    "available": true,
    "minOrderQty": 0.5
  }
]
```

---

### GET `/products/{id}`
**Access:** Any authenticated
**When to call:** Customer clicks on a product card

---

### POST `/products`
**Access:** ADMIN
**When to call:** Admin creates product

Request:
```json
{
  "name": "Tomatoes",
  "description": "Fresh farm tomatoes",
  "categoryId": 1,
  "unit": "kg",
  "pricePerUnit": 40.00,
  "imageUrl": "https://cloudinary.com/...",
  "available": true,
  "minOrderQty": 0.5
}
```

---

### PUT `/products/{id}`
**Access:** ADMIN
**When to call:** Admin edits product

---

### PUT `/products/{id}/availability`
**Access:** ADMIN
**When to call:** Admin toggles product availability for the week

Request:
```json
{ "available": false }
```

---

## 7. Weekly Cycle

### GET `/cycles/current`
**Access:** Any authenticated
**When to call:** Customer home page load, before placing order to check if cycle is OPEN

Response:
```json
{
  "id": 5,
  "cycleLabel": "Week of 12 Jan 2025",
  "status": "OPEN",
  "orderOpenAt": "2025-01-13T00:00:00",
  "orderCloseAt": "2025-01-15T14:00:00",
  "deliveryDateSat": "2025-01-18",
  "deliveryDateSun": "2025-01-19",
  "timeRemainingSeconds": 172800
}
```
`timeRemainingSeconds` → use for countdown timer on home screen.
Returns `null` if no active cycle.

---

### GET `/cycles`
**Access:** ADMIN
**When to call:** Admin cycle management page

---

### POST `/cycles`
**Access:** ADMIN
**When to call:** Admin manually creates a cycle

Request:
```json
{
  "cycleLabel": "Week of 12 Jan 2025",
  "orderOpenAt": "2025-01-13T00:00:00",
  "orderCloseAt": "2025-01-15T14:00:00",
  "deliveryDateSat": "2025-01-18",
  "deliveryDateSun": "2025-01-19"
}
```

---

### PUT `/cycles/{id}/open`
**Access:** ADMIN
**When to call:** Admin force-opens ordering

---

### PUT `/cycles/{id}/close`
**Access:** ADMIN
**When to call:** Admin force-closes ordering (triggers procurement aggregation)

---

### PUT `/cycles/{id}/status`
**Access:** ADMIN
**When to call:** Admin moves cycle to PROCUREMENT / DELIVERING / COMPLETED

Request:
```json
{ "status": "PROCUREMENT" }
```
Valid values: `OPEN`, `CLOSED`, `PROCUREMENT`, `DELIVERING`, `COMPLETED`

---

## 8. Orders — Customer

### POST `/orders`
**Access:** ROLE_CUSTOMER
**When to call:** Customer confirms checkout

Request:
```json
{
  "items": [
    { "productId": 1, "quantity": 2.5 },
    { "productId": 3, "quantity": 1.0 }
  ],
  "notes": "Please leave at door"
}
```
Response:
```json
{
  "id": 101,
  "orderNumber": "WB-2025-0001",
  "status": "PLACED",
  "totalAmount": 180.00,
  "referralDiscount": 0.00,
  "amountToCollect": 180.00,
  "paymentMethod": "COD",
  "paymentStatus": "PENDING",
  "items": [...]
}
```
Errors: `400` cycle not open, product unavailable, quantity below minimum

---

### GET `/orders/my`
**Access:** ROLE_CUSTOMER
**When to call:** Customer order history page load

Response: Array of order summaries

---

### GET `/orders/my/{id}`
**Access:** ROLE_CUSTOMER
**When to call:** Customer clicks on an order

Response: Full order with items + delivery slot

---

### PUT `/orders/{id}/cancel`
**Access:** ROLE_CUSTOMER
**When to call:** Customer clicks Cancel on an order (only while cycle is OPEN)

---

## 9. Orders — Admin

### GET `/orders`
**Access:** ADMIN
**When to call:** Admin orders list page

Query params: `?cycleId={id}` to filter by cycle

---

### GET `/orders/{id}`
**Access:** ADMIN
**When to call:** Admin clicks on an order row

---

### PUT `/orders/{id}/status`
**Access:** ADMIN
**When to call:** Admin updates order status

Request:
```json
{ "status": "CONFIRMED" }
```
Valid values: `PLACED`, `CONFIRMED`, `PACKED`, `DELIVERED`, `CANCELLED`

---

### GET `/orders/cycle/{cycleId}`
**Access:** ADMIN
**When to call:** Admin views all orders for a specific cycle (packing list view)

---

## 10. Procurement

### GET `/procurement/{cycleId}`
**Access:** ADMIN
**When to call:** Admin opens procurement page for a cycle

Response:
```json
{
  "cycleLabel": "Week of 12 Jan 2025",
  "totalOrders": 87,
  "items": [
    {
      "productId": 1,
      "productName": "Tomatoes",
      "unit": "kg",
      "totalQuantity": 240.0,
      "vendorName": "Kadapa Mandi",
      "vendorNotes": "",
      "procuredQty": 245.0,
      "status": "PROCURED"
    }
  ]
}
```

---

### PUT `/procurement/{cycleId}/{productId}`
**Access:** ADMIN
**When to call:** Admin fills in vendor name / procured qty inline

Request:
```json
{
  "vendorName": "Kadapa Mandi",
  "vendorNotes": "Buy extra 5kg buffer",
  "procuredQty": 245.0,
  "status": "PROCURED"
}
```

---

### GET `/procurement/{cycleId}/export`
**Access:** ADMIN
**When to call:** Admin clicks Export Excel button

Response: Binary `.xlsx` file download
Angular: Use `HttpClient` with `responseType: 'blob'` and trigger browser download.

---

## 11. Transport Tracking

### GET `/transport/{cycleId}`
**Access:** ADMIN
**When to call:** Admin transport tracking page load

Response:
```json
[
  {
    "id": 1,
    "stage": "PROCUREMENT_STARTED",
    "notes": "Started purchasing at Kadapa market",
    "updatedBy": "admin",
    "createdOn": "2025-01-15T10:00:00"
  }
]
```

---

### POST `/transport/{cycleId}/stage`
**Access:** ADMIN
**When to call:** Admin adds a new transport stage update

Request:
```json
{
  "stage": "IN_TRANSIT",
  "notes": "Truck left Kadapa at 6 AM"
}
```
Valid stages: `PROCUREMENT_STARTED`, `GOODS_LOADED`, `IN_TRANSIT`, `ARRIVED`, `PACKING`, `DISPATCHED`

---

## 12. Delivery Batches

### GET `/batches/{cycleId}`
**Access:** ADMIN
**When to call:** Admin delivery management page

---

### POST `/batches`
**Access:** ADMIN
**When to call:** Admin creates a delivery batch

Request:
```json
{
  "cycleId": 5,
  "batchLabel": "Block A — Saturday",
  "deliveryDate": "2025-01-18",
  "orderIds": [101, 102, 103]
}
```

---

### PUT `/batches/{id}/assign`
**Access:** ADMIN
**When to call:** Admin assigns delivery staff to a batch

Request:
```json
{ "assignedTo": 42 }
```
`assignedTo` = userId of delivery staff

---

### GET `/batches/{id}/orders`
**Access:** ADMIN, DELIVERY
**When to call:** Delivery staff views their assigned batch

---

### PUT `/batches/{id}/orders/{orderId}/delivered`
**Access:** DELIVERY
**When to call:** Delivery staff marks an order as delivered

---

## 13. Notifications

### GET `/notifications/my`
**Access:** Any authenticated
**When to call:** Notification bell click, notification page load

Response:
```json
[
  {
    "id": 10,
    "title": "Order Placed!",
    "body": "Order #WB-2025-0001 placed. Total: ₹450",
    "type": "ORDER_UPDATE",
    "readStatus": false,
    "sentAt": "2025-01-13T10:30:00"
  }
]
```

---

### PUT `/notifications/{id}/read`
**Access:** Any authenticated
**When to call:** User opens a notification

---

### PUT `/notifications/read-all`
**Access:** Any authenticated
**When to call:** User clicks "Mark all as read"

---

### POST `/notifications/broadcast`
**Access:** ADMIN
**When to call:** Admin sends announcement to all users

Request:
```json
{
  "title": "This week's ordering is now open!",
  "body": "Order by Wednesday 2 PM. Fresh vegetables from Kadapa.",
  "type": "ANNOUNCEMENT"
}
```

---

### POST `/notifications/send`
**Access:** ADMIN
**When to call:** Admin sends notification to a specific user

Request:
```json
{
  "userId": 42,
  "title": "Your order is confirmed",
  "body": "Order #WB-2025-0001 has been confirmed.",
  "type": "ORDER_UPDATE"
}
```

---

## 14. Referrals

### GET `/referrals/my-code`
**Access:** ROLE_CUSTOMER
**When to call:** Customer referral page load

Response:
```json
{ "referralCode": "ABC12345" }
```

---

### GET `/referrals/my`
**Access:** ROLE_CUSTOMER
**When to call:** Customer referral history page

Response:
```json
[
  {
    "id": 1,
    "referredPhone": "98765XXXXX",
    "status": "REWARDED",
    "createdOn": "2025-01-10T10:00:00"
  }
]
```

---

### POST `/referrals/apply`
**Access:** ROLE_CUSTOMER
**When to call:** New user applies referral code at signup (called from verify-otp flow via `referralCode` field)

Note: Referral code is passed directly in the `verify-otp` request body — no separate API call needed.

---

## 15. Masters (Lookup Values)

### GET `/masters/{type}`
**Access:** Any authenticated
**When to call:** Populate dropdowns on page load

Valid types:
- `ORDER_STATUS`
- `PAYMENT_METHOD`
- `PAYMENT_STATUS`
- `DELIVERY_SLOT`
- `CYCLE_STATUS`
- `TRANSPORT_STAGE`
- `NOTIFICATION_TYPE`

Response:
```json
[
  { "id": 1, "lookupValue": "1", "lookupItem": "Saturday", "lookupCode": "SAT", "type": "DELIVERY_SLOT" },
  { "id": 2, "lookupValue": "2", "lookupItem": "Sunday", "lookupCode": "SUN", "type": "DELIVERY_SLOT" }
]
```
Frontend stores `lookupCode` in requests, displays `lookupItem` in UI.

---

### POST `/masters`
**Access:** ADMIN
**When to call:** Admin adds a new lookup value

Request:
```json
{
  "lookupValue": "3",
  "lookupItem": "Express",
  "lookupCode": "EXPRESS",
  "type": "DELIVERY_SLOT"
}
```

---

### PUT `/masters/{id}`
**Access:** ADMIN
**When to call:** Admin edits a lookup value

---

## 16. Dashboard

### GET `/dashboard/summary`
**Access:** ADMIN
**When to call:** Admin dashboard page load

Response:
```json
{
  "currentCycleId": 5,
  "currentCycleLabel": "Week of 12 Jan 2025",
  "totalOrdersThisCycle": 87,
  "totalRevenueThisCycle": 45230.00,
  "pendingDeliveries": 23,
  "openCycle": true
}
```

---

### GET `/dashboard/cycle/{cycleId}`
**Access:** ADMIN
**When to call:** Admin views stats for a specific past cycle

---

## 17. Community Feedback

### GET `/feedback`
**Access:** Public (or Any auth — TBD)
**When to call:** Feedback wall page load

Response:
```json
[
  {
    "id": 1,
    "customerName": "Ravi K.",
    "flatNumber": "A-204",
    "rating": 5,
    "message": "Fresh vegetables every week!",
    "isPinned": true,
    "helpfulCount": 12,
    "postedAt": "2025-01-13T10:30:00",
    "adminReply": {
      "message": "Thank you Ravi!",
      "repliedAt": "2025-01-13T11:00:00"
    }
  }
]
```

---

### POST `/feedback`
**Access:** ROLE_CUSTOMER
**When to call:** Customer submits feedback form

Request:
```json
{ "rating": 5, "message": "Fresh vegetables every week!" }
```

---

### PUT `/feedback/{id}`
**Access:** ROLE_CUSTOMER (own feedback, within 24h)
**When to call:** Customer edits their feedback

---

### POST `/feedback/{id}/helpful`
**Access:** ROLE_CUSTOMER
**When to call:** Customer clicks "Helpful" on a feedback

---

### POST `/feedback/{id}/reply`
**Access:** ADMIN
**When to call:** Admin replies to a feedback

Request:
```json
{ "message": "Thank you for your feedback!" }
```

---

### PUT `/feedback/{id}/pin`
**Access:** ADMIN
**When to call:** Admin pins a feedback to top

---

### PUT `/feedback/{id}/flag`
**Access:** ADMIN
**When to call:** Admin flags inappropriate feedback

---

### GET `/feedback/admin`
**Access:** ADMIN
**When to call:** Admin feedback management page (shows flagged too)
