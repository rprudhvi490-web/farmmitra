# FarmMitra — Phase 10 Improvements (Client Meeting)

## Overview
New requirements identified in client review meeting. Covers order model changes, notification UX overhaul, product descriptions, and admin user management improvements.

---

## Issue 1 — Multiple Orders Per Customer Per Cycle ✅ Priority: CRITICAL

### Problem
`DuplicateOrderRule` throws an error if a customer tries to place a second order in the same cycle. Client wants customers to be able to place **multiple separate orders** in the same cycle (e.g. forgot to add something, wants to top up).

### Current behaviour
`existsByUserIdAndCycleId` → throws "You have already placed an order for this week."

### What changes
- **Remove `DuplicateOrderRule` entirely** — no more one-order-per-cycle restriction
- Each order gets its own `orderNumber` — a customer can have WB-2025-0001 and WB-2025-0002 in the same cycle
- Admin orders page already shows all orders per cycle — no change needed there
- Procurement sheet already aggregates by product across all orders — no change needed there
- Stock limit check (`CycleProductService.reserveStock`) already sums across all orders — still correct

### Files
- `DuplicateOrderRule.java` — delete
- No other backend changes needed

---

## Issue 2 — Reorder Confirmation Popup ✅ Priority: HIGH

### Problem
Reorder button immediately adds items to cart and navigates away with no warning. Customer doesn't know what's about to happen.

### What changes
- On clicking Reorder, show a **confirmation dialog** (Angular Material Dialog):
  - Title: "Reorder — {orderNumber}"
  - Body: list of items being added (name + qty)
  - Note: "These items will be **appended** to your current cart (existing items are not removed)"
  - Buttons: "Cancel" | "Yes, Add to Cart"
- Only on confirm → add to cart → show snackbar → navigate to home

### Files
- `orders.component.ts` + `orders.component.html` — add dialog trigger
- `order-detail.component.ts` + `order-detail.component.html` — same
- No backend changes

---

## Issue 3 — Notification Banner in App (Inline, not Snackbar) ✅ Priority: HIGH

### Problem
All current notifications (success, error, info) use Angular Material Snackbar which appears at the bottom. Client wants:
- Notifications appear **below the navbar** (top of page)
- Auto-close after configurable time (default 15 seconds) for success/info
- Failure/error notifications must **NOT auto-close** — only manually closable
- Configurable duration in `environment.ts`

### What changes
- Create a **global `NotificationBannerComponent`** — a fixed banner below the navbar
- Create a **`ToastService`** (replaces direct Snackbar calls) with methods:
  - `success(message)` — green, auto-closes after `notifDuration` seconds
  - `error(message)` — red, never auto-closes, X button only
  - `info(message)` — blue, auto-closes
  - `warning(message)` — amber, auto-closes
- Replace all `MatSnackBar` calls across customer + admin screens with `ToastService`
- Add to `environment.ts`: `notificationDurationMs: 15000`

### Files
- New: `core/components/notification-banner/notification-banner.component.ts/html/scss`
- New: `core/services/toast.service.ts`
- `environment.ts` — add `notificationDurationMs`
- `customer-layout.component.html` — add `<app-notification-banner>` below navbar
- `admin-layout.component.html` — add `<app-notification-banner>` below toolbar
- All components using `MatSnackBar` → replace with `ToastService`

---

## Issue 4 — Order Status Notification Banner on Customer Landing Page ✅ Priority: HIGH

### Problem
When admin updates transport stages (IN_TRANSIT, PACKING, DISPATCHED etc.), a notification is saved in the DB and sent to the customer. But the customer only sees it if they navigate to the notifications page. Client wants:
- When customer opens the app (home page), if there are **unread ORDER_UPDATE notifications**, show them **automatically in the banner** one by one
- Banner shows: order number + status message
- Auto-closes after 15 seconds
- Once shown (auto-closed OR manually closed) → mark as read → never show again on next visit
- Only ORDER_UPDATE type notifications trigger this auto-show behaviour

### What changes
- `customer-layout.component.ts` — on init, fetch unread ORDER_UPDATE notifications, feed them to `ToastService` sequentially with a small delay between each
- Mark each as read via `PUT /notifications/{id}/read` when shown
- This reuses the same `NotificationBannerComponent` from Issue 3

### Files
- `customer-layout.component.ts` — add unread ORDER_UPDATE fetch + auto-show logic
- `notification.service.ts` (customer) — add `getUnreadOrderUpdates()` method
- Backend already sends ORDER_UPDATE notifications on transport stage changes ✅

---

## Issue 5 — Fix Admin Notification Send (broadcast + sendToUser) ✅ Priority: HIGH

### Problem
Admin notifications page — "Send to User" and "Broadcast" buttons are not working. Likely a DTO mismatch or endpoint issue.

### Investigation needed
- Check `SendNotificationRequest` DTO — does it have `userId` as required or optional?
- `sendToUser` endpoint expects `userId` — admin UI might be sending wrong field name
- `broadcast` endpoint — check if Angular service is hitting the right URL

### Files
- `NotificationController.java` — review `SendNotificationRequest` DTO
- `admin.services.ts` — `NotificationAdminService.sendToUser()` and `broadcast()`
- `notifications.component.ts` (admin) — check what payload is being sent

---

## Issue 6 — Product Special Description for Customers ✅ Priority: MEDIUM

### Problem
Admin wants to add a **special marketing description** per product (quality highlights, source info, freshness claims) that is shown to customers on the product card/detail. This is different from the existing plain `description` field.

### What changes
**Backend**
- Add `specialDescription TEXT` column to `product` table — `V6__add_product_special_description.sql`
- Add field to `Product` model, `CreateProductRequest`, `ProductResponse`
- `ProductService` — include in create/update/toResponse

**Admin UI**
- Add "Special Description" textarea to product form (below existing description)
- Label: "Marketing Highlight (shown to customers)"

**Customer UI**
- Show `specialDescription` on product card as a small italicised tagline below the price
- Only show if non-empty

### Files
- `V6__add_product_special_description.sql`
- `Product.java`, `ProductDto.java`, `ProductService.java`
- `products.component.html/ts` (admin)
- `home.component.html/scss` (customer)

---

## Issue 7 — Admin Users Page: Fix Role Assignment + Active Sessions ✅ Priority: HIGH

### Problem A — Role assignment broken
Admin users page "Assign Role" is not working correctly. Likely the Angular component is sending wrong payload or the role list is hardcoded incorrectly.

### Problem B — Active device/session info missing
Admin wants to see for each customer:
- How many devices (tokens) are currently active (not invalidated, not expired)
- If any token was used in the last 5 minutes → show "Active Now" badge

### Current state
- `InvalidatedToken` table tracks logged-out tokens
- JWT expiry is 10 hours (OTP login) or 7 days (password login)
- No tracking of which tokens were issued per user or last-used time

### What changes for Problem A
- Check `AssignRoleRequest` DTO and admin UI payload
- Check if role removal (unassign) is needed alongside assign

### What changes for Problem B
**Backend**
- New table `user_token` — tracks issued tokens per user: `(id, user_id, token_hash, issued_at, last_used_at, expired_at, device_hint)`
- On login (OTP + password) → save token record
- `JwtFilter` → on each request, update `last_used_at` for matching token
- New endpoint `GET /users/{id}/sessions` → returns active session list
- Active = not in `invalidated_token` AND `expired_at > now`
- "Active Now" = `last_used_at > now - 5 mins`
- On logout → mark token as invalidated (already done) + update `user_token` record

**Admin UI**
- Users table — add session count column: "X active sessions" badge
- Clicking it opens a detail panel/dialog showing device list with last-used time and "Active Now" indicator

### Files
**Backend**
- `V7__add_user_token_tracking.sql`
- New: `UserToken.java` model
- New: `UserTokenRepository.java`
- `AuthService.java` (or wherever login happens) — save token on issue
- `JwtFilter.java` — update `last_used_at`
- `UserController.java` — add `GET /{id}/sessions`
- `UserService.java` — add `getActiveSessions(userId)`

**Admin UI**
- `admin.services.ts` — add `getUserSessions(id)`
- `users.component.ts/html/scss` — show session count + fix role assignment

---

## Common Infrastructure — What Can Be Done Together

| Group | Issues | Shared work |
|---|---|---|
| **Notification system overhaul** | 3, 4, 5 | Build `ToastService` + `NotificationBannerComponent` first, then wire Issues 4 and 5 on top |
| **Product model extension** | 6 | Simple DB + backend + UI, standalone |
| **Order rule change** | 1 | One-line backend change, standalone |
| **Reorder dialog** | 2 | Pure frontend, standalone |
| **User session tracking** | 7B | Needs new table + backend + UI, biggest effort |
| **Role assignment fix** | 7A | Investigate first, likely a small fix |

---

## Implementation Order

| # | Issue | Effort | Dependencies | Priority |
|---|---|---|---|---|
| 1 | Remove duplicate order restriction | XS | None | CRITICAL |
| 2 | Reorder confirmation dialog | S | None | HIGH |
| 3 | ToastService + NotificationBanner component | M | None — do first, others build on it | HIGH |
| 4 | Auto-show ORDER_UPDATE on customer home | S | Needs #3 | HIGH |
| 5 | Fix admin notification send | S | Needs #3 | HIGH |
| 6 | Product special description | S | None | MEDIUM |
| 7A | Fix role assignment in admin users | S | None | HIGH |
| 7B | User session / active login tracking | L | None | MEDIUM |

---

## Status

| # | Issue | Status |
|---|---|---|
| 1 | Multiple orders per cycle | ✅ Done |
| 2 | Reorder confirmation dialog | ✅ Done |
| 3 | ToastService + banner | ✅ Done |
| 4 | Auto-show order status on home | ✅ Done |
| 5 | Fix admin notification send | ✅ Done |
| 6 | Product special description | ✅ Done |
| 7A | Fix role assignment | ✅ Done |
| 7B | User session tracking | ✅ Done |

---

## From Previous Phase (Still Pending)

| Task | Status |
|---|---|
| Dashboard `/history` + `/customers` endpoints — controller done ✅, Angular UI pending | 🟡 Partial |
| Mobile responsive — admin screens (tables, forms) | ✅ Done |
| Mobile responsive — customer screens | ✅ Done |
| Merge home + products | ✅ Done |
| Reorder button | ✅ Done (dialog pending — Issue 2 above) |
| Product ratings | ✅ Done |
| Cycle stock limits | ✅ Done |
| Buffer time / concurrent order safety | ✅ Done |
