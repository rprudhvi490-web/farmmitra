# FarmMitra — Firebase Phone Auth Migration
## Complete Change Analysis

**Goal:** Replace backend-generated OTP (dev logs / AWS SNS) with Firebase Phone Authentication.
Firebase handles SMS delivery. Spring Boot validates the Firebase ID token and issues its own JWT.
The password login tab stays exactly as-is — zero changes needed there.

---

## What Changes vs What Stays the Same

| Component | Change? | Why |
|-----------|---------|-----|
| Firebase project setup | ✅ NEW | Enable Phone Auth in Firebase console |
| Angular `@angular/fire` package | ✅ NEW | Firebase SDK for Angular |
| Angular `app.config.ts` | ✅ CHANGE | Register Firebase providers |
| Angular `login.component.ts` | ✅ CHANGE | Use Firebase `signInWithPhoneNumber` |
| Angular `verify.component.ts` | ✅ CHANGE | Use Firebase `confirmationResult.confirm()` |
| Angular `auth.service.ts` | ✅ CHANGE | Remove `sendOtp()`, add `firebaseLogin()` |
| Spring Boot `pom.xml` | ✅ CHANGE | Add `firebase-admin` dependency |
| Spring Boot `FirebaseConfig.java` | ✅ NEW | Initialize Firebase Admin SDK |
| Spring Boot `AuthController.java` | ✅ CHANGE | Add `POST /auth/firebase-login` endpoint |
| Spring Boot `AuthService.java` | ✅ CHANGE | Add `firebaseLogin()` method, remove `sendOtp()` |
| Spring Boot `SecurityConfig.java` | ✅ CHANGE | Permit `/api/auth/firebase-login` |
| Spring Boot `OtpService.java` | 🗑 DELETE | No longer needed |
| Spring Boot `OtpSendStrategy.java` | 🗑 DELETE | No longer needed |
| Spring Boot `DevOtpStrategy.java` | 🗑 DELETE | No longer needed |
| Spring Boot `AwsSnsOtpStrategy.java` | 🗑 DELETE | No longer needed |
| Spring Boot `OtpVerification` model | 🗑 DELETE | Table no longer needed |
| Spring Boot `OtpVerificationRepository` | 🗑 DELETE | No longer needed |
| Spring Boot `OtpExpiredException` | 🗑 DELETE | No longer needed |
| Spring Boot `OtpMaxAttemptsException` | 🗑 DELETE | No longer needed |
| DB `otp_verification` table | 🗑 DROP | Firebase owns OTP now |
| `application.properties` | ✅ CHANGE | Remove otp.* and AWS SNS props, add firebase config |
| Password login flow | ✅ UNCHANGED | Stays exactly as-is |
| JWT generation after login | ✅ UNCHANGED | Spring Boot still issues its own JWT |
| All other backend code | ✅ UNCHANGED | Orders, cycles, delivery etc. untouched |

---

## Pre-requisites (Do These First)

### Step 0A — Firebase Console Setup
1. Go to https://console.firebase.google.com
2. Select your existing Firebase project (you already have Firebase Admin SDK — FcmService exists)
3. Go to **Authentication** → **Sign-in method**
4. Enable **Phone** provider
5. Under **Phone** → **Phone numbers for testing** → add test numbers:
   - `+916305262393` → code `123456` (your admin number for dev testing)
   - Add any other test numbers you use
6. These test numbers get OTP verified without real SMS — free forever

### Step 0B — Firebase Service Account JSON
You likely already have this since FcmService exists.
Check `backend/src/main/resources/` for a file like `firebase-service-account.json` or `google-services.json`.

If not:
1. Firebase Console → Project Settings → Service Accounts
2. Click **Generate new private key** → download JSON
3. Place it at: `backend/src/main/resources/firebase-service-account.json`
4. Add to `.gitignore`: `**/firebase-service-account.json`

### Step 0C — Firebase Web Config
1. Firebase Console → Project Settings → General → Your apps
2. Find your web app (or add one) → copy the config object:
```javascript
const firebaseConfig = {
  apiKey: "...",
  authDomain: "...",
  projectId: "...",
  storageBucket: "...",
  messagingSenderId: "...",
  appId: "..."
};
```
You'll need this for the Angular environment files.

---

## Part 1 — Angular Changes

### Change 1.1 — Install Firebase packages
```bash
cd C:\WeekendBasket\UI
npm install @angular/fire firebase
```

### Change 1.2 — Environment files
**File: `UI/src/environments/environment.ts`**
```typescript
// ADD firebaseConfig object
export const environment = {
  production: false,
  apiBaseUrl: '/weekendbasket/api',
  firebase: {
    apiKey: 'YOUR_API_KEY',
    authDomain: 'YOUR_PROJECT.firebaseapp.com',
    projectId: 'YOUR_PROJECT_ID',
    storageBucket: 'YOUR_PROJECT.appspot.com',
    messagingSenderId: 'YOUR_SENDER_ID',
    appId: 'YOUR_APP_ID'
  }
};
```

**File: `UI/src/environments/environment.prod.ts`**
```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://farmmitra-backend.onrender.com/weekendbasket/api',
  firebase: {
    apiKey: 'YOUR_API_KEY',
    authDomain: 'YOUR_PROJECT.firebaseapp.com',
    projectId: 'YOUR_PROJECT_ID',
    storageBucket: 'YOUR_PROJECT.appspot.com',
    messagingSenderId: 'YOUR_SENDER_ID',
    appId: 'YOUR_APP_ID'
  }
};
```

### Change 1.3 — Register Firebase in app.config.ts
**File: `UI/src/app/app.config.ts`**

Add these imports:
```typescript
import { provideFirebaseApp, initializeApp } from '@angular/fire/app';
import { provideAuth, getAuth } from '@angular/fire/auth';
import { environment } from '../environments/environment';
```

Add to `providers` array:
```typescript
provideFirebaseApp(() => initializeApp(environment.firebase)),
provideAuth(() => getAuth()),
```

### Change 1.4 — Update auth.service.ts
**File: `UI/src/app/core/services/auth.service.ts`**

Remove `sendOtp()` method entirely.
Add new `firebaseLogin()` method:

```typescript
import { Auth, RecaptchaVerifier, signInWithPhoneNumber, ConfirmationResult } from '@angular/fire/auth';

// Add to constructor injection:
private firebaseAuth = inject(Auth);

// ADD: store confirmation result for use in verify step
private confirmationResult: ConfirmationResult | null = null;
private recaptchaVerifier: RecaptchaVerifier | null = null;

// ADD: initiate Firebase phone auth
async sendFirebaseOtp(phoneNumber: string): Promise<void> {
  if (!this.recaptchaVerifier) {
    this.recaptchaVerifier = new RecaptchaVerifier(this.firebaseAuth, 'recaptcha-container', {
      size: 'invisible'
    });
  }
  const e164 = '+91' + phoneNumber;
  this.confirmationResult = await signInWithPhoneNumber(this.firebaseAuth, e164, this.recaptchaVerifier);
}

// ADD: verify OTP with Firebase, then exchange for app JWT
async verifyFirebaseOtp(otp: string, referralCode?: string): Promise<AuthResponse> {
  if (!this.confirmationResult) throw new Error('No OTP sent yet');
  const credential = await this.confirmationResult.confirm(otp);
  const firebaseToken = await credential.user.getIdToken();

  return firstValueFrom(
    this.http.post<AuthResponse>(`${this.base}/auth/firebase-login`, {
      token: firebaseToken,
      referralCode: referralCode ?? null
    }).pipe(
      tap(res => {
        this.tokenService.save(res.token);
        this.tokenService.saveUsername(res.username);
      })
    )
  );
}
```

Add import: `import { firstValueFrom } from 'rxjs';`

### Change 1.5 — Update login.component.ts
**File: `UI/src/app/auth/login/login.component.ts`**

Replace the `sendOtp()` method:
```typescript
async sendOtp(): Promise<void> {
  if (this.phone.invalid) { this.phone.markAsTouched(); return; }
  this.loading.set(true);
  try {
    await this.authService.sendFirebaseOtp(this.phone.value!);
    this.loading.set(false);
    this.router.navigate(['/auth/verify'], { state: { phone: this.phone.value } });
  } catch (err: any) {
    this.loading.set(false);
    console.error('Firebase OTP error', err);
  }
}
```

### Change 1.6 — Add recaptcha container to login.component.html
**File: `UI/src/app/auth/login/login.component.html`**

Add at the very bottom of the template (invisible, required by Firebase):
```html
<!-- Required by Firebase reCAPTCHA — invisible, do not remove -->
<div id="recaptcha-container"></div>
```

### Change 1.7 — Update verify.component.ts
**File: `UI/src/app/auth/verify/verify.component.ts`** (or wherever OTP verification happens)

Replace the current `verifyOtp()` method that calls `POST /auth/verify-otp`:
```typescript
async verifyOtp(): Promise<void> {
  const otp = this.getOtpValue(); // however you collect the 6 digits
  this.loading.set(true);
  try {
    const res = await this.authService.verifyFirebaseOtp(otp, this.referralCode);
    this.loading.set(false);
    this.authService.redirectAfterLogin(res.isNewUser);
  } catch (err: any) {
    this.loading.set(false);
    // Show error — wrong OTP, expired etc.
  }
}
```

Remove the resend OTP button or update it to call `authService.sendFirebaseOtp()` again.

---

## Part 2 — Spring Boot Backend Changes

### Change 2.1 — Add firebase-admin dependency
**File: `backend/pom.xml`**

Add inside `<dependencies>`:
```xml
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.4.3</version>
</dependency>
```

### Change 2.2 — Firebase Admin initialization
**New File: `backend/src/main/java/com/weekendbasket/app/config/FirebaseConfig.java`**

```java
package com.weekendbasket.app.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Configuration;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LogManager.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount =
                        getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized");
            }
        } catch (Exception e) {
            log.error("Firebase Admin SDK initialization failed: {}", e.getMessage());
        }
    }
}
```

### Change 2.3 — Add FirebaseLoginRequest DTO
**File: `backend/src/main/java/com/weekendbasket/app/dto/AuthDto.java`**

Add new record:
```java
public record FirebaseLoginRequest(
    @NotBlank(message = "Firebase token is required") String token,
    String referralCode
) {}
```

### Change 2.4 — Add firebaseLogin() to AuthService
**File: `backend/src/main/java/com/weekendbasket/app/service/AuthService.java`**

Add import:
```java
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
```

Add method (keep `verifyOtp()` temporarily for backward compat if needed):
```java
@Transactional
public AuthResponse firebaseLogin(FirebaseLoginRequest request) {
    try {
        // Verify token with Firebase — throws if invalid/expired
        FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(request.token());
        // Firebase stores phone as "+919876543210" — strip +91
        String rawPhone = decoded.getPhoneNumber();
        String phoneNumber = rawPhone.startsWith("+91") ? rawPhone.substring(3) : rawPhone;

        boolean isNewUser = !userRepository.existsByPhoneNumber(phoneNumber);
        User user;

        if (isNewUser) {
            user = registerNewUser(phoneNumber);
            if (request.referralCode() != null && !request.referralCode().isBlank()) {
                try {
                    referralService.applyReferral(phoneNumber, request.referralCode());
                } catch (Exception e) {
                    log.warn("Referral apply failed for {}: {}", phoneNumber, e.getMessage());
                }
            }
        } else {
            user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new WeekendBasketException("Your account is blocked. Please contact support.");
        }

        List<String> roles = loadRoles(user.getId());
        String token = jwtUtil.generateToken(user.getPhoneNumber(), roles, otpExpiryMs);
        saveTokenRecord(user, token, otpExpiryMs, null);

        log.info("Firebase login success: {}", phoneNumber);
        return new AuthResponse(token, otpExpiryMs, user.getPhoneNumber(),
                user.getUsername(), roles, isNewUser, user.getHasPassword());

    } catch (WeekendBasketException e) {
        throw e;
    } catch (Exception e) {
        log.error("Firebase token verification failed: {}", e.getMessage());
        throw new WeekendBasketException("Invalid or expired Firebase token.");
    }
}
```

### Change 2.5 — Add endpoint to AuthController
**File: `backend/src/main/java/com/weekendbasket/app/controller/AuthController.java`**

Add:
```java
@PostMapping("/firebase-login")
public ResponseEntity<AuthResponse> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request) {
    return ResponseEntity.ok(authService.firebaseLogin(request));
}
```

Add import: `import com.weekendbasket.app.dto.AuthDto.FirebaseLoginRequest;`

### Change 2.6 — Permit firebase-login in SecurityConfig
**File: `backend/src/main/java/com/weekendbasket/app/config/SecurityConfig.java`**

`/api/auth/**` already covers `/api/auth/firebase-login` — **no change needed here**.

### Change 2.7 — Update application.properties
**File: `backend/src/main/resources/application.properties`**

Remove these properties (no longer needed):
```properties
# REMOVE these:
otp.expiry.minutes=5
otp.max.attempts=3
otp.provider=dev
aws.access.key.id=...
aws.secret.access.key=...
aws.sns.region=...
```

Keep: All other properties unchanged.

---

## Part 3 — Cleanup (Do After Testing)

### Files to Delete After Firebase OTP Works

| File | Reason |
|------|--------|
| `backend/.../service/OtpService.java` | Firebase handles OTP now |
| `backend/.../otp/OtpSendStrategy.java` | Interface no longer needed |
| `backend/.../otp/DevOtpStrategy.java` | Dev logging not needed |
| `backend/.../otp/AwsSnsOtpStrategy.java` | AWS SNS replaced by Firebase |
| `backend/.../model/OtpVerification.java` | Table being dropped |
| `backend/.../repository/OtpVerificationRepository.java` | Model deleted |
| `backend/.../exception/OtpExpiredException.java` | Firebase handles errors |
| `backend/.../exception/OtpMaxAttemptsException.java` | Firebase handles errors |
| `backend/.../scheduler/CleanupScheduler.java` (otp part) | Remove otp cleanup cron only |

### Also Remove from AuthService.java
- The `sendOtp()` method
- The `OtpService` injection
- The `otpService.verifyOtp()` call in `verifyOtp()`
- The entire `verifyOtp(VerifyOtpRequest)` method

### Also Remove from AuthController.java
- `POST /auth/send-otp` endpoint
- `POST /auth/verify-otp` endpoint

### Database Migration
**New file: `backend/src/main/resources/db/migration/V8__drop_otp_table.sql`**
```sql
DROP TABLE IF EXISTS otp_verification;
```
⚠️ Only add this migration AFTER the app is fully working with Firebase. Never rollback Flyway versioned migrations.

---

## Part 4 — Rate Limiting Update

Current `RateLimitFilter` rate-limits `send-otp` by phone number.
After migration, Firebase handles rate limiting on their side — remove the per-phone OTP limiter.

**File: `backend/src/main/java/com/weekendbasket/app/filters/RateLimitFilter.java`**

Remove or comment out the OTP-specific rate limit block.
Keep the general IP-based rate limit (protects all public endpoints).

---

## Implementation Order (Step by Step)

| Step | What to do | Risk |
|------|-----------|------|
| 1 | Firebase Console: enable Phone Auth, add test numbers | Zero — no code change |
| 2 | Download service account JSON, place in resources/ | Zero |
| 3 | Add firebase-admin to pom.xml, build to verify | Low |
| 4 | Create FirebaseConfig.java — test app starts | Low |
| 5 | Add FirebaseLoginRequest DTO | Zero |
| 6 | Add firebaseLogin() to AuthService | Low |
| 7 | Add POST /auth/firebase-login endpoint | Low |
| 8 | npm install @angular/fire firebase | Low |
| 9 | Add firebase config to environments | Low |
| 10 | Register Firebase in app.config.ts | Low |
| 11 | Update auth.service.ts — add sendFirebaseOtp + verifyFirebaseOtp | Medium |
| 12 | Update login.component.ts — use async/await | Medium |
| 13 | Add recaptcha-container div to login HTML | Low |
| 14 | Update verify.component.ts | Medium |
| 15 | Test end-to-end with test phone numbers | — |
| 16 | Delete old OTP files after testing passes | Low |
| 17 | Add V8 DB migration to drop otp_verification | Low |

---

## Key Points to Remember

1. **Phone format** — Firebase sends `+919876543210`, your DB stores `9876543210`. Strip `+91` in `firebaseLogin()`.

2. **Recaptcha container** — The `<div id="recaptcha-container">` must exist in the DOM when `RecaptchaVerifier` is created. It's invisible but required by Firebase to prevent SMS abuse.

3. **Test numbers** — Add your phone numbers as Firebase test numbers (console → Auth → Phone → Test numbers). These bypass real SMS and are completely free.

4. **Resend OTP** — Just call `authService.sendFirebaseOtp()` again. Firebase automatically handles cooldown.

5. **Password login unchanged** — The `POST /auth/login` (password tab) is completely untouched.

6. **Your JWT is still yours** — Firebase only verifies the phone. After that, Spring Boot issues your own JWT exactly as before. Nothing else in the app changes.

7. **Firebase Admin SDK already exists** — You already have `firebase-admin` referenced in FcmService. Check if `FirebaseApp` is already initialized there. If yes, skip Change 2.2.

---

## Status Tracker

| Step | Task | Status |
|------|------|--------|
| 0A | Enable Firebase Phone Auth in console | 🔴 Not Started |
| 0B | Download service account JSON | 🔴 Not Started |
| 0C | Get Firebase web config | 🔴 Not Started |
| 2.1 | Add firebase-admin to pom.xml | 🔴 Not Started |
| 2.2 | Create FirebaseConfig.java | 🔴 Not Started |
| 2.3 | Add FirebaseLoginRequest DTO | 🔴 Not Started |
| 2.4 | Add firebaseLogin() to AuthService | 🔴 Not Started |
| 2.5 | Add endpoint to AuthController | 🔴 Not Started |
| 2.7 | Clean up application.properties | 🔴 Not Started |
| 1.1 | npm install @angular/fire firebase | 🔴 Not Started |
| 1.2 | Update environment files | 🔴 Not Started |
| 1.3 | Register Firebase in app.config.ts | 🔴 Not Started |
| 1.4 | Update auth.service.ts | 🔴 Not Started |
| 1.5 | Update login.component.ts | 🔴 Not Started |
| 1.6 | Add recaptcha div to login HTML | 🔴 Not Started |
| 1.7 | Update verify.component.ts | 🔴 Not Started |
| 3 | Delete old OTP files + DB migration | 🔴 Not Started |
