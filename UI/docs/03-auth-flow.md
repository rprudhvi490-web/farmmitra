# Auth Flow — OTP Login, JWT, Guards

## Overview

WeekendBasket uses phone number + OTP authentication. No username/password.
After OTP verification, backend returns a JWT token valid for 10 hours.
Angular stores the token in localStorage and attaches it to every API request.

---

## Login Flow (Step by Step)

```
1. User opens /auth/login
   → Enters phone number (10 digits)
   → Clicks "Send OTP"
   → Angular calls POST /api/auth/send-otp

2. Backend generates 6-digit OTP, stores in DB (5 min expiry)
   → Returns { message: "OTP sent successfully" }
   → Angular navigates to /auth/verify (passes phone via router state)

3. User enters 6-digit OTP
   → Clicks "Verify"
   → Angular calls POST /api/auth/verify-otp

4. Backend validates OTP
   → Returns { token, roles, isNewUser, phoneNumber, username }
   → Angular:
       a. Stores token in localStorage key: 'wb_token'
       b. Decodes token to extract roles
       c. If isNewUser = true → navigate to /customer/profile (complete profile)
       d. If roles includes ROLE_ADMIN or ROLE_SUPER_ADMIN → navigate to /admin/dashboard
       e. If roles includes ROLE_CUSTOMER → navigate to /customer/home
       f. If roles includes ROLE_DELIVERY → navigate to /admin/batches

5. Every subsequent API call:
   → AuthInterceptor reads token from localStorage
   → Adds header: Authorization: Bearer <token>
```

---

## JWT Token Structure

```
Header.Payload.Signature
```

Decoded payload:
```json
{
  "sub": "9876543210",
  "roles": ["ROLE_CUSTOMER"],
  "phone": "9876543210",
  "iat": 1705123200,
  "exp": 1705159200
}
```

Token expiry: 10 hours from issue time.

---

## Token Storage

| Key         | Value              | Storage      |
|-------------|--------------------|--------------|
| `wb_token`  | JWT string         | localStorage |

On logout: remove `wb_token` from localStorage, call `POST /api/auth/logout`.

---

## Angular Services

### `TokenService`

```typescript
// Responsibilities:
// - save(token: string): void
// - getToken(): string | null
// - removeToken(): void
// - getRoles(): string[]
// - getPhone(): string
// - isExpired(): boolean
// - isLoggedIn(): boolean
```

Uses `jwt-decode` to decode the token and extract claims.

---

### `AuthService`

```typescript
// Responsibilities:
// - sendOtp(phone: string): Observable<any>
// - verifyOtp(phone: string, otp: string, referralCode?: string): Observable<AuthResponse>
// - logout(): Observable<any>
// - getCurrentUser(): Observable<UserProfile>
// - hasRole(role: string): boolean
// - isAdmin(): boolean
// - isCustomer(): boolean
```

---

## HTTP Interceptors

### `AuthInterceptor`

Attaches JWT to every outgoing request:

```typescript
// Reads token from TokenService
// Adds: Authorization: Bearer <token>
// Skips: /auth/send-otp, /auth/verify-otp, /health (public endpoints)
```

### `ErrorInterceptor`

Handles HTTP errors globally:

```typescript
// 401 Unauthorized → clear token → redirect to /auth/login
// 403 Forbidden    → show "Access denied" snackbar
// 400 Bad Request  → show error message from response body
// 429 Too Many Req → show "Too many attempts. Try again later."
// 500 Server Error → show "Something went wrong. Please try again."
```

---

## Route Guards

### `AuthGuard` (CanActivate)

Protects all routes that require login:

```typescript
// Check: TokenService.isLoggedIn() && !TokenService.isExpired()
// If not logged in → redirect to /auth/login
// Applied to: /customer/**, /admin/**
```

### `RoleGuard` (CanActivate)

Protects role-specific routes:

```typescript
// Check: TokenService.getRoles() includes required role
// If not authorized → redirect to appropriate home screen
// Applied to: /admin/** (requires ROLE_ADMIN or ROLE_SUPER_ADMIN)
```

### `GuestGuard` (CanActivate)

Prevents logged-in users from accessing login page:

```typescript
// If already logged in → redirect to home based on role
// Applied to: /auth/**
```

---

## Routing with Guards

```typescript
const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  {
    path: 'auth',
    canActivate: [GuestGuard],
    loadChildren: () => import('./auth/auth.module')
  },
  {
    path: 'customer',
    canActivate: [AuthGuard],
    loadChildren: () => import('./customer/customer.module')
  },
  {
    path: 'admin',
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_DELIVERY'] },
    loadChildren: () => import('./admin/admin.module')
  },
  { path: '**', redirectTo: '/auth/login' }
];
```

---

## OTP Input UX

- 6 individual input boxes (one digit each)
- Auto-focus moves to next box on digit entry
- Backspace moves focus to previous box
- Auto-submit when all 6 digits filled
- Resend OTP button with 60-second cooldown timer
- Error shown inline: "Invalid OTP" / "OTP expired" / "Too many attempts"

---

## Session Expiry Handling

- Token expires after 10 hours
- `ErrorInterceptor` catches `401` → clears token → redirects to login
- On app load: `AuthGuard` checks `isExpired()` → redirects if expired
- No silent token refresh (MVP) — user re-logs in after expiry

---

## New User Flow

When `isNewUser = true` in verify-otp response:
1. Navigate to `/customer/profile`
2. Show "Complete your profile" form (name, flat number, block)
3. Call `PUT /api/users/me`
4. On save → navigate to `/customer/home`
5. Profile completion is optional — user can skip and fill later
