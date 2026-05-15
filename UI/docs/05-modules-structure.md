# Angular Modules & Folder Structure

## Module Breakdown

```
src/app/
├── core/                          ← Singleton services, interceptors, guards
│   ├── interceptors/
│   │   ├── auth.interceptor.ts    ← Attach JWT to every request
│   │   └── error.interceptor.ts   ← Global HTTP error handling
│   ├── guards/
│   │   ├── auth.guard.ts          ← Redirect to login if not authenticated
│   │   ├── role.guard.ts          ← Redirect if wrong role
│   │   └── guest.guard.ts         ← Redirect to home if already logged in
│   ├── services/
│   │   ├── auth.service.ts        ← sendOtp, verifyOtp, logout
│   │   ├── token.service.ts       ← JWT storage, decode, expiry check
│   │   └── master.service.ts      ← GET /masters/:type (cached)
│   └── core.module.ts
│
├── shared/                        ← Reusable components, pipes, directives
│   ├── components/
│   │   ├── otp-input/             ← 6-box OTP input component
│   │   ├── countdown-timer/       ← Live countdown from timeRemainingSeconds
│   │   ├── order-status-badge/    ← Coloured chip for order status
│   │   ├── transport-timeline/    ← Visual stage progress bar
│   │   ├── notification-bell/     ← Bell icon with unread badge
│   │   ├── confirm-dialog/        ← Reusable MatDialog confirmation
│   │   └── loading-spinner/       ← Full-page and inline spinner
│   ├── pipes/
│   │   ├── inr-currency.pipe.ts   ← Format numbers as ₹1,234.00
│   │   └── time-ago.pipe.ts       ← "2 hours ago" relative time
│   └── shared.module.ts
│
├── auth/                          ← Lazy loaded
│   ├── login/
│   │   ├── login.component.ts
│   │   └── login.component.html
│   ├── verify-otp/
│   │   ├── verify-otp.component.ts
│   │   └── verify-otp.component.html
│   └── auth.module.ts
│
├── customer/                      ← Lazy loaded, ROLE_CUSTOMER guard
│   ├── layout/
│   │   └── customer-layout.component.ts   ← Top nav + router outlet
│   ├── home/
│   ├── products/
│   │   ├── product-list/
│   │   └── product-detail/
│   ├── cart/
│   ├── checkout/
│   ├── orders/
│   │   ├── order-list/
│   │   └── order-detail/
│   ├── notifications/
│   ├── profile/
│   ├── referral/
│   ├── feedback/
│   ├── services/
│   │   ├── product.service.ts
│   │   ├── order.service.ts
│   │   ├── cart.service.ts        ← Local cart state (BehaviorSubject)
│   │   ├── notification.service.ts
│   │   ├── referral.service.ts
│   │   └── feedback.service.ts
│   └── customer.module.ts
│
├── admin/                         ← Lazy loaded, ROLE_ADMIN guard
│   ├── layout/
│   │   └── admin-layout.component.ts      ← Sidenav + toolbar + router outlet
│   ├── dashboard/
│   ├── cycles/
│   ├── products/
│   ├── categories/
│   ├── orders/
│   │   ├── order-list/
│   │   └── order-detail/
│   ├── procurement/
│   ├── transport/
│   ├── batches/
│   │   ├── batch-list/
│   │   └── batch-orders/
│   ├── users/
│   ├── notifications/
│   ├── feedback/
│   ├── masters/
│   ├── communities/
│   ├── services/
│   │   ├── cycle.service.ts
│   │   ├── procurement.service.ts
│   │   ├── transport.service.ts
│   │   ├── batch.service.ts
│   │   ├── user-admin.service.ts
│   │   ├── dashboard.service.ts
│   │   └── community.service.ts
│   └── admin.module.ts
│
└── app.module.ts                  ← Root module, imports CoreModule
```

---

## Service Responsibilities

### Core Services (singleton, provided in root)

| Service | Methods |
|---------|---------|
| `AuthService` | `sendOtp()`, `verifyOtp()`, `logout()` |
| `TokenService` | `save()`, `get()`, `remove()`, `getRoles()`, `isExpired()`, `isLoggedIn()` |
| `MasterService` | `getByType(type)` — cached with `shareReplay(1)` |

### Customer Services

| Service | Methods |
|---------|---------|
| `ProductService` | `getAll(params?)`, `getById(id)` |
| `OrderService` | `place(req)`, `getMy()`, `getMyById(id)`, `cancel(id)` |
| `CartService` | `add()`, `remove()`, `clear()`, `getItems()`, `getTotal()` — BehaviorSubject |
| `NotificationService` | `getMy()`, `markRead(id)`, `markAllRead()` |
| `ReferralService` | `getMyCode()`, `getMy()` |
| `FeedbackService` | `getAll()`, `post(req)`, `markHelpful(id)` |

### Admin Services

| Service | Methods |
|---------|---------|
| `CycleService` | `getAll()`, `getCurrent()`, `create()`, `open(id)`, `close(id)`, `updateStatus(id, status)` |
| `ProcurementService` | `getSheet(cycleId)`, `update(cycleId, productId, req)`, `export(cycleId)` |
| `TransportService` | `getLog(cycleId)`, `addStage(cycleId, req)` |
| `BatchService` | `getForCycle(cycleId)`, `create(req)`, `assign(id, req)`, `getOrders(id)`, `markDelivered(batchId, orderId)` |
| `UserAdminService` | `getAll()`, `getById(id)`, `block(id)`, `unblock(id)`, `assignRole(id, req)` |
| `DashboardService` | `getSummary()`, `getCycleSummary(cycleId)` |
| `CommunityService` | `getAll()`, `create(req)`, `update(id, req)` |

---

## Cart State (Local — No API)

`CartService` uses `BehaviorSubject<CartItem[]>`:

```typescript
interface CartItem {
  productId: number;
  productName: string;
  unit: string;
  pricePerUnit: number;
  quantity: number;
  totalPrice: number;
}
```

Cart is cleared on:
- Successful order placement
- Logout
- Page refresh (not persisted — MVP)

---

## HTTP Base Service Pattern

All services extend or use a base pattern:

```typescript
// All API calls use environment.apiBaseUrl as base
// All return Observable<T>
// Error handling delegated to ErrorInterceptor
```

---

## Shared Module Exports

`SharedModule` exports everything needed by both Customer and Admin modules:
- All Angular Material modules used
- `OtpInputComponent`
- `CountdownTimerComponent`
- `OrderStatusBadgeComponent`
- `TransportTimelineComponent`
- `NotificationBellComponent`
- `ConfirmDialogComponent`
- `LoadingSpinnerComponent`
- `InrCurrencyPipe`
- `TimeAgoPipe`
- `ReactiveFormsModule`
- `CommonModule`
