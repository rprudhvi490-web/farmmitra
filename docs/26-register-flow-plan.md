# Plan: New User Registration (Option 1 — Separate Register Page)

## Current State

| Flow | Status |
|---|---|
| Login (password) | ✅ `/auth/login` |
| Forgot Password (OTP) | ✅ `/auth/forgot-password` → `/auth/verify` |
| Verify OTP | ✅ `/auth/verify` (supports `mode: login \| forgot-password`) |
| **Register (new user)** | ❌ Missing |

The `POST /api/auth/firebase-login` backend endpoint already handles new user creation
(`isNewUser` logic in `AuthService.firebaseLogin()`). We just need the frontend entry point.

---

## Target Flow

```
/auth/login
  └─ "New User? Register" link
        ↓
  /auth/register
    - Enter mobile number
    - Optional: referral code
    - Backend check: if number already exists → show error (don't burn OTP quota)
    - Send Firebase OTP
        ↓
  /auth/verify  (mode = 'register')
    - Verify OTP
    - On success → isNewUser=true → redirect to /customer/profile (set name/password)
```

---

## Changes Required

### 1. Backend — `AuthController.java`
Add `POST /api/auth/register/check` endpoint:
- Validates phone format
- Returns `409 CONFLICT` if user **already exists** (opposite of forgot-password check)
- If new number → increments OTP quota and returns ALLOWED
- This prevents quota burn for duplicate registrations

### 2. Frontend — New `register` component (`UI/src/app/auth/register/`)
**Files to create:**
- `register.component.ts` — phone input + optional referral code, calls `sendFirebaseOtp(phone, 'register')`
- `register.component.html` — form UI matching app theme
- `register.component.scss` — styles matching login/forgot-password pages

**Logic:**
- On submit: call `authService.sendFirebaseOtp(phone, 'register')`
- Navigate to `/auth/verify` with state `{ phone, referralCode, mode: 'register' }`
- Error handling: 409 → "This number is already registered. Please login.", 429 → quota message

### 3. Frontend — `auth.service.ts`
Update `sendFirebaseOtp()` mode type and check URL:
- Add `'register'` mode → calls `POST /api/auth/register/check` (validates number is NEW)
- Current `'register'` mode calls `GET /api/auth/check-quota` (no user existence check) → **replace** with the new dedicated endpoint

### 4. Frontend — `verify-otp.component.ts`
- Add `'register'` to the mode type union: `'login' | 'forgot-password' | 'register'`
- `'register'` mode after verify: use existing `redirectAfterLogin(res.isNewUser)` — no change needed since `isNewUser` will be `true`
- Update `resendOtp()` to pass the current mode when resending

### 5. Frontend — `auth.routes.ts`
Add route:
```ts
{ path: 'register', loadComponent: () => import('./register/register.component').then(m => m.RegisterComponent) }
```

### 6. Frontend — `login.component.html`
Add "New User? Register" link below the existing "Forgot Password?" link in `.bottom-links`.

---

## File Change Summary

| File | Action |
|---|---|
| `AuthController.java` | Add `POST /api/auth/register/check` |
| `auth/register/register.component.ts` | **Create** |
| `auth/register/register.component.html` | **Create** |
| `auth/register/register.component.scss` | **Create** |
| `auth.service.ts` | Update mode type + register check URL |
| `verify-otp.component.ts` | Add `'register'` to mode union, fix `resendOtp()` mode |
| `auth.routes.ts` | Add register route |
| `login.component.html` | Add register link |

---

## OTP Quota Usage After All Changes

| Action | OTP Consumed? | Backend Check |
|---|---|---|
| Login (password) | ❌ No | — |
| Register new number | ✅ Yes | `register/check` → 409 if exists |
| Forgot password | ✅ Yes | `forgot-password/check` → 404 if not exists |
| Duplicate registration attempt | ❌ No (blocked at check) | Returns 409 before Firebase call |
| Non-existent number forgot-password | ❌ No (blocked at check) | Returns 404 before Firebase call |

---

## UI Mockup (Login Page Bottom Links)

```
[ Login ]

Forgot Password?    New User? Register
```
