# Angular Web Application — Plan

## Why Web First

Build and validate the complete end-to-end flow on Angular web before building the React Native mobile app.
- Faster iteration — no emulator/device needed
- All API contracts validated in browser first
- Admin screens are better suited for web anyway
- Mobile app reuses the same backend — zero rework

---

## Folder Structure

```
c:\WeekendBasket\
├── backend\          ← Spring Boot (existing)
├── ui\               ← Angular workspace (new)
│   ├── src\
│   │   ├── app\
│   │   │   ├── core\          ← interceptors, guards, services
│   │   │   ├── shared\        ← reusable components, pipes
│   │   │   ├── auth\          ← OTP login
│   │   │   ├── customer\      ← customer-facing screens
│   │   │   └── admin\         ← admin-facing screens
│   │   ├── assets\
│   │   └── environments\
│   ├── angular.json
│   └── package.json
└── docs\
```

---

## Tech Stack

| Layer         | Technology                        |
|---------------|-----------------------------------|
| Framework     | Angular (latest)                  |
| UI Library    | Angular Material                  |
| HTTP          | Angular HttpClient                |
| Routing       | Angular Router with lazy loading  |
| State         | RxJS + Services (no NgRx for MVP) |
| Auth Guard    | CanActivate + JWT decode          |
| Build         | Angular CLI                       |
| Node          | Already installed                 |

---

## Environment Config

```typescript
// environments/environment.ts (dev)
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:9090/weekendbasket/api'
};

// environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://<render-url>/weekendbasket/api'
};
```

---

## Core Module

| File | Purpose |
|------|---------|
| `auth.interceptor.ts` | Attach `Authorization: Bearer <token>` to every request |
| `error.interceptor.ts` | Global HTTP error handling (401 → redirect to login, 400 → toast) |
| `auth.guard.ts` | Protect routes — redirect to login if no token |
| `role.guard.ts` | Protect admin routes — redirect if not ADMIN/SUPER_ADMIN |
| `auth.service.ts` | Login, logout, token storage, current user |
| `token.service.ts` | Store/retrieve JWT from localStorage, decode claims |

---

## Modules & Screens

### Auth Module (`/auth`)

| Screen | Route | Description |
|--------|-------|-------------|
| Login | `/auth/login` | Enter phone number → send OTP |
| Verify OTP | `/auth/verify` | Enter 6-digit OTP → get JWT |

Flow:
1. User enters phone → `POST /api/auth/send-otp`
2. OTP input screen → `POST /api/auth/verify-otp`
3. On success → store JWT → redirect based on role:
   - CUSTOMER → `/customer/home`
   - ADMIN / SUPER_ADMIN → `/admin/dashboard`

---

### Customer Module (`/customer`) — ROLE_CUSTOMER

| Screen | Route | API |
|--------|-------|-----|
| Home | `/customer/home` | `GET /cycles/current` |
| Products | `/customer/products` | `GET /products`, `GET /categories` |
| Product Detail | `/customer/products/:id` | `GET /products/:id` |
| Cart | `/customer/cart` | Local state |
| Place Order | `/customer/checkout` | `POST /orders` |
| My Orders | `/customer/orders` | `GET /orders/my` |
| Order Detail | `/customer/orders/:id` | `GET /orders/my/:id` |
| Notifications | `/customer/notifications` | `GET /notifications/my` |
| Profile | `/customer/profile` | `GET /users/me`, `PUT /users/me` |
| Referral | `/customer/referral` | `GET /referrals/my-code`, `GET /referrals/my` |
| Feedback Wall | `/customer/feedback` | `GET /feedback` |

---

### Admin Module (`/admin`) — ROLE_ADMIN

| Screen | Route | API |
|--------|-------|-----|
| Dashboard | `/admin/dashboard` | `GET /dashboard/summary` |
| Weekly Cycle | `/admin/cycles` | `GET /cycles`, `PUT /cycles/:id/open`, `/close` |
| Products | `/admin/products` | `GET /products`, `POST`, `PUT` |
| Categories | `/admin/categories` | `GET /categories`, `POST`, `PUT` |
| Orders | `/admin/orders` | `GET /orders`, `PUT /orders/:id/status` |
| Procurement | `/admin/procurement/:cycleId` | `GET /procurement/:cycleId`, `PUT`, export |
| Transport | `/admin/transport/:cycleId` | `GET /transport/:cycleId`, `POST /stage` |
| Delivery Batches | `/admin/batches/:cycleId` | `GET /batches/:cycleId`, `POST`, assign |
| Users | `/admin/users` | `GET /users`, block/unblock, assign roles |
| Notifications | `/admin/notifications` | `POST /notifications/broadcast` |
| Feedback | `/admin/feedback` | `GET /feedback/admin`, reply, pin, flag |
| Masters | `/admin/masters` | `GET /masters/:type`, `POST`, `PUT` |
| Communities | `/admin/communities` | `GET /communities`, `POST`, `PUT` |

---

## Shared Components

| Component | Purpose |
|-----------|---------|
| `otp-input` | 6-box OTP input with auto-focus |
| `countdown-timer` | Live countdown to cycle close |
| `order-status-badge` | Coloured badge for order status |
| `transport-timeline` | Visual stage progress bar |
| `notification-bell` | Bell icon with unread count badge |
| `confirm-dialog` | Reusable confirmation modal |
| `toast` | Success/error toast notifications |
| `loading-spinner` | Full-page and inline loaders |

---

## Routing Structure

```
/                     → redirect to /auth/login
/auth/login           → LoginComponent (public)
/auth/verify          → VerifyOtpComponent (public)
/customer/**          → CustomerModule (lazy, ROLE_CUSTOMER guard)
/admin/**             → AdminModule (lazy, ROLE_ADMIN guard)
**                    → redirect to /auth/login
```

---

## Implementation Phases (Angular)

### Phase 9.1 — Setup & Core
- [ ] Install Angular CLI globally
- [ ] Create Angular workspace under `ui/`
- [ ] Install Angular Material
- [ ] Setup environments (dev + prod API URLs)
- [ ] Core module: interceptors, guards, auth service
- [ ] App routing with lazy loading

### Phase 9.2 — Auth Flow
- [ ] Login screen (phone input)
- [ ] OTP verify screen (6-digit input)
- [ ] JWT storage + role-based redirect
- [ ] Logout

### Phase 9.3 — Customer Screens
- [ ] Home (cycle status + countdown timer)
- [ ] Product browse + category filter
- [ ] Cart (local state)
- [ ] Checkout + place order
- [ ] My orders + order detail
- [ ] Profile view/edit

### Phase 9.4 — Admin Core
- [ ] Dashboard (weekly stats)
- [ ] Weekly cycle management (open/close/status)
- [ ] Product + category management
- [ ] Order list + status update

### Phase 9.5 — Admin Operations
- [ ] Procurement sheet view + vendor update
- [ ] Excel export button
- [ ] Transport stage updates
- [ ] Delivery batch management

### Phase 9.6 — Polish
- [ ] Notifications (customer + admin broadcast)
- [ ] Feedback wall (customer post + admin reply)
- [ ] Referral screen
- [ ] Responsive layout (desktop + tablet)
- [ ] Error handling + loading states throughout

---

## CORS Configuration (Backend)

Backend needs to allow Angular dev server origin.
Add to `SecurityConfig.java`:

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:4200"));
    config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return new UrlBasedCorsConfigurationSource() {{ registerCorsConfiguration("/**", config); }};
}
```

---

## Setup Commands

```bash
# Install Angular CLI
npm install -g @angular/cli

# Create workspace
cd c:\WeekendBasket
ng new ui --routing --style=scss --skip-git

# Add Angular Material
cd ui
ng add @angular/material

# Serve dev
ng serve --proxy-config proxy.conf.json
```

---

## Current Status

- Phase 9.1: ✅ Complete
- Phase 9.2: ✅ Complete
- Phase 9.3: ✅ Complete
- Phase 9.4: ✅ Complete
- Phase 9.5: ✅ Complete
- Phase 9.6: ✅ Complete
- Phase 9.7: 🟡 In Progress
- Phase 9.8: 🔴 Not Started
- Phase 9.9: 🔴 Not Started
