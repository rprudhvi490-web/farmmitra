# FarmMitra — Phase 11 Improvements

## Overview
Requirements from latest review session covering banner UX, admin orders default sort, cancel order protection, cart stock validation, and special description styling.

---

## Issue 1 — Banner Auto-Close Timings ✅ Priority: HIGH

### Problem
Success banners auto-close after 15s (too long). Error banners never auto-close — only manually closable.

### What changes
- Success → auto-close after **5 seconds**
- Error → auto-close after **10 seconds** (also still manually closable)
- Both types remain manually closable via X button
- Update `environment.ts`: `notificationDurationMs: 5000`, `notificationErrorDurationMs: 10000`
- `toast.service.ts`: pass duration to `push()` for error type using `notificationErrorDurationMs`

### Files
- `environment.ts` — update durations
- `core/services/toast.service.ts` — wire error auto-close

---

## Issue 2 — Admin Orders Default to Latest Week ✅ Priority: HIGH

### Problem
Admin `/admin/orders` page loads cycles and auto-selects `c[0]` — which is the oldest cycle (API returns ascending order).

### What changes
- Select the **last** cycle in the array as default (latest week first)
- `admin/orders/orders.component.ts` — change `c[0]` → `c[c.length - 1]`

### Files
- `admin/orders/orders.component.ts`

---

## Issue 3 — Cancel Order: Disabled After Procurement Started ✅ Priority: HIGH

### Problem
Once the cycle moves past PLACED (procurement started), the customer can still see and click "Cancel Order". The button should be **disabled** with a message: "Procurement started — to cancel call customer support: <number>".

Even if someone bypasses the UI and hits the API, the backend must reject the cancel with a proper error.

### What changes

**Frontend**
- `order-detail.component.html` — show cancel button for `PLACED` status only when cycle is `OPEN`
- When cycle is `CLOSED`/`PROCUREMENT`/etc and order is `PLACED` → show disabled button with tooltip/message: "Procurement is started, you cannot cancel this order. To cancel, call customer support: +91-XXXXXXXXXX"

**Backend**
- `OrderService.cancelOrder()` — check cycle status; if cycle is not `OPEN`, throw `WeekendBasketException("Procurement has started. To cancel your order, please call customer support.")`
- This ensures even UI-bypassed API calls are rejected

### Files
- `order-detail.component.html` + `order-detail.component.ts` — disable logic + message
- `OrderService.java` — cycle status check before cancel

---

## Issue 4 — Cart: Stock Limit Warnings Instead of Silent Add ✅ Priority: HIGH

### Problem
When a user clicks "Add to Cart" on a product:
1. If `maxStock` is set and limit is reached (product `soldOut`) → item silently adds to cart, but checkout will fail
2. If no `maxStock` is configured → no feedback

### What changes

**Customer UI (`home.component.ts`)**
- `addToCart()` — before adding, check `product.soldOut` (need to expose from `CycleProductResponse` via product API or store cycle-product state)
- If `soldOut === true` → show `toast.warning('Stock Reached Limit — kindly try again later')` and **do not add to cart**
- If `maxStock` is null/0 (no limit configured) → show `toast.warning('Re-evaluating stock for this item — kindly try after some time')` and **do not add to cart**

**Backend — product API must expose stock state**
- `ProductResponse` / `ProductService.getAll()` for customer needs to include `soldOut: boolean` and `stockConfigured: boolean` for the current cycle
- Join with `cycle_product` table for current open cycle

### Files
- `customer.services.ts` — add `soldOut`, `stockConfigured` fields to `Product` interface
- `home.component.ts` — add guard in `addToCart()`
- `ProductService.java` / `ProductDto.java` — include cycle stock state in product list response
- `ProductController.java` / `ProductRepository.java` — query current cycle stock

---

## Issue 5 — Special Description Color: Red → Green ✅ Priority: LOW

### Problem
`.product-tagline` in `home.component.scss` uses `color: var(--fm-accent)` which resolves to red. Should be green.

### What changes
- `home.component.scss` — change `.product-tagline` color to `var(--fm-primary)` (green)

### Files
- `home.component.scss`

---

## Issue 6 — Admin Cancel Order (Any State Before DELIVERED) ✅ Priority: HIGH

### Problem
Admin had no way to cancel an order on behalf of a customer who called in after procurement started. The state machine only allowed cancel from `PLACED` via `CUSTOMER_CANCEL` and `CONFIRMED` via `ADMIN_UPDATE`.

### What changes
- `order-states.json` — added `ADMIN_CANCEL` transitions from `PLACED`, `CONFIRMED`, `PACKED` → `CANCELLED`
- `OrderController.java` — new `PUT /orders/{id}/cancel` endpoint (admin only)
- `OrderService.java` — `adminCancelOrder()` uses `ADMIN_CANCEL`, always calls `releaseStock()`
- `admin.services.ts` — added `adminCancel()` method
- `orders.component.html/ts` — added Cancel column with red icon button; hidden for terminal states (`CANCELLED`, `DELIVERED`)

### Note on stock release
`releaseStock()` is always called regardless of cycle status:
- Cycle `OPEN` → freed stock is usable by other customers ✅ meaningful
- Cycle `CLOSED/PROCUREMENT/DELIVERING` → stock numbers updated for accuracy, but no new orders can use it since cycle is not open — harmless, keeps data clean

### Files
- `order-states.json`
- `OrderController.java`, `OrderService.java`
- `admin.services.ts`
- `admin/orders/orders.component.html/ts/scss`

---

## Issue 7 — Remove Re-Open Button for Closed Cycles ✅ Priority: LOW

### Problem
`CLOSED → OPEN` transition was never defined in the state machine (one-way flow by design). The Re-Open button in admin cycles UI was triggering a state machine error.

### What changes
- `cycles.component.html` — removed Re-Open button for `CLOSED` cycles

### Files
- `admin/cycles/cycles.component.html`

---

## Implementation Order

| # | Issue | Effort | Status |
|---|---|---|---|
| 1 | Banner auto-close timings (success 5s, error 10s) | XS | ✅ Done |
| 2 | Special description color red → green | XS | ✅ Done |
| 3 | Admin orders default to latest week | XS | ✅ Done |
| 4 | Cancel order — disabled UI + backend guard | S | ✅ Done |
| 5 | Cart stock warnings (sold out + no stock config) | M | ✅ Done |
| 6 | Admin cancel order (any state) | S | ✅ Done |
| 7 | Remove Re-Open button | XS | ✅ Done |

---

## Status

| # | Issue | Status |
|---|---|---|
| 1 | Banner timings | ✅ Done |
| 2 | Special description green | ✅ Done |
| 3 | Admin orders latest week default | ✅ Done |
| 4 | Cancel order protection | ✅ Done |
| 5 | Cart stock warnings | ✅ Done |
| 6 | Admin cancel order | ✅ Done |
| 7 | Remove Re-Open button | ✅ Done |
