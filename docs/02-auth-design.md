# Authentication & Authorization Design

## Auth Method

WeekendBasket uses **Phone Number + OTP** login (no username/password).
After OTP verification, a JWT token is issued — same JWT flow as ElderCare.

---

## Flow

```
POST /api/auth/send-otp
  → AuthController
  → OtpService.sendOtp(phoneNumber)
      → generate 6-digit OTP
      → store in otp_verification table (expires in 5 min)
      → send via Firebase Phone Auth / SMS provider
  → returns { message: "OTP sent" }

POST /api/auth/verify-otp
  → AuthController
  → OtpService.verifyOtp(phoneNumber, otp)
      → validate OTP + expiry
      → load or create User record
      → JwtUtil.generateToken(userId, roles)
  → returns { token, expiresIn, roles, isNewUser }

Every subsequent request:
  → JwtFilter (OncePerRequestFilter)
      → extract Bearer token
      → validate token (signature + expiry)
      → check invalidated_token table (logout blacklist)
      → set SecurityContext with authorities (ROLE_CUSTOMER etc.)
  → Spring Security evaluates @PreAuthorize on controller method
```

---

## Tables Involved

- `app_user` — phone number, user status, role
- `user_profile` — name, email, flat number, community
- `role` — role catalogue (ROLE_CUSTOMER, ROLE_ADMIN, etc.)
- `role_access` — maps user → role (many-to-many bridge)
- `otp_verification` — stores OTP + expiry per phone number
- `invalidated_token` — JWT logout blacklist

---

## Role Hierarchy (SecurityConfig)

```
ROLE_SUPER_ADMIN > ROLE_ADMIN
ROLE_ADMIN > ROLE_DELIVERY
ROLE_ADMIN > ROLE_CUSTOMER
```

- ADMIN automatically has all CUSTOMER and DELIVERY permissions.
- SUPER_ADMIN has everything.
- Adding a new elevated role = one line in hierarchy, zero endpoint annotation changes.

---

## JWT Details

| Property   | Value                                              |
|------------|----------------------------------------------------|
| Library    | jjwt 0.12.x                                        |
| Algorithm  | HS256                                              |
| Secret     | Externalized in `application.properties` (jwt.secret) |
| Expiry     | 10 hours (configurable)                            |
| Claims     | `sub` = userId, `roles` = list, `phone` = phone number |

Token blacklist (logout) stored in `invalidated_token` table, cleaned up by scheduled job.

---

## OTP Details

| Property       | Value                          |
|----------------|--------------------------------|
| Length         | 6 digits                       |
| Expiry         | 5 minutes                      |
| Max attempts   | 3 (then lock for 15 min)       |
| Storage        | `otp_verification` table       |
| Provider       | AWS SNS (dev mode during development) |

---

## Password Login (Optional)

Users can optionally set a password to avoid OTP every login.

| Property | Value |
|----------|-------|
| Set via  | `PUT /api/auth/set-password` (requires active JWT) |
| Login via | `POST /api/auth/login` with phone + password |
| Token expiry | 7 days (vs 10 hours for OTP) |
| Flag | `hasPassword: true` on user after set |
| Storage | BCrypt hash in `app_user.password` |

**Flow:**
1. User logs in with OTP (first time or any time)
2. On profile screen → "Set Password" option
3. User sets password → `hasPassword = true`
4. Next login: user can choose OTP or password
5. Login screen shows both options when `hasPassword = true`

**Endpoints:**
```
POST /api/auth/login          → { phoneNumber, password } → JWT (7 days)
PUT  /api/auth/set-password   → { password, confirmPassword } → 204
```

---

## Endpoints

| Method | Path                    | Access        | Description                    |
|--------|-------------------------|---------------|--------------------------------|
| POST   | /api/auth/send-otp      | Public        | Send OTP to phone number       |
| POST   | /api/auth/verify-otp    | Public        | Verify OTP, returns JWT        |
| POST   | /api/auth/logout        | Authenticated | Blacklist token                |
| GET    | /api/health             | Public        | Health check                   |
| GET    | /actuator/**            | Public (dev) / ADMIN (prod) | Actuator |

---

## New User vs Returning User

On `verify-otp`:
- If phone number exists in `app_user` → login flow → return token
- If phone number is new → auto-register → create `app_user` + `user_profile` (empty) → assign `ROLE_CUSTOMER` → return token with `isNewUser: true`
- Mobile app uses `isNewUser` flag to redirect to profile completion screen

---

## Password Policy

No passwords. OTP-only authentication.
Admin accounts created directly by SUPER_ADMIN via internal API (no OTP needed for admin creation).

---

## Global Exception Handler

`@RestControllerAdvice` on `GlobalExceptionHandler` catches:

| Exception                         | HTTP Status |
|-----------------------------------|-------------|
| `WeekendBasketException`          | 400         |
| `ResourceNotFoundException`       | 404         |
| `AccessDeniedException`           | 403         |
| `AuthenticationException`         | 401         |
| `OtpExpiredException`             | 400         |
| `OtpMaxAttemptsException`         | 429         |
| `MethodArgumentNotValidException` | 422         |
| `Exception` (fallback)            | 500         |

All error responses:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "OTP has expired",
  "timestamp": "2025-01-01T10:00:00"
}
```
