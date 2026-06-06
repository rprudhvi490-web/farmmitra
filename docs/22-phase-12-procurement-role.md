# FarmMitra — Phase 12: Procurement Role & Email Automation

## Overview
New operational role for field procurement staff. They need to receive the procurement Excel by email when the cycle closes, and have restricted access to view/update the procurement sheet — without full admin access.

---

## Issue 1 — PROCUREMENT Role

### Problem
Currently only ADMIN can access the procurement sheet. But the actual people buying from Kadapa market are field staff — they shouldn't have full admin access (products, users, orders, cycles).

### What changes

**Backend**
- Add `ROLE_PROCUREMENT` to `SecurityConfig.java` hierarchy: `ROLE_ADMIN > ROLE_PROCUREMENT`
- Update `ProcurementController` — change `hasRole('ADMIN')` to `hasAnyRole('ADMIN', 'PROCUREMENT')`
- No new DB migration needed for the role itself — roles are stored in `role` table, created via API

**Admin UI**
- Admin can assign `PROCUREMENT` role to users via existing users page
- User with PROCUREMENT role logs in via phone OTP like any other user

### Files
- `SecurityConfig.java`
- `ProcurementController.java`

---

## Issue 2 — Email Field on User Profile

### Problem
Procurement staff need an email address to receive the Excel. Currently `user_profile` has no email field.

### What changes

**Backend**
- `V8__add_user_email.sql` — `ALTER TABLE user_profile ADD COLUMN email VARCHAR(255)`
- `UserProfile.java` — add `email` field
- `UserProfileDto.java` — add `email` to request/response records
- `UserProfileService.java` — include in create/update/toResponse
- `UserController.java` — profile update already handles user_profile, no endpoint change needed

**Admin UI**
- User detail / create form — add email input field

### Files
- `V8__add_user_email.sql`
- `UserProfile.java`, `UserProfileDto.java`, `UserProfileService.java`
- `users.component.html/ts` (admin)

---

## Issue 3 — Auto Email Procurement Excel on Cycle Close

### Problem
Procurement staff currently have to manually log in and export the Excel. This is error-prone and adds friction. When cycle closes, they should automatically receive the Excel by email.

### What changes

**Backend**
- `ProcurementEmailService.java` — generates Excel (reuses existing POI logic), sends as email attachment
  - Fetches all users with `ROLE_PROCUREMENT` who have a non-null email
  - Sends using `JavaMailSender` (Spring Mail)
  - Subject: `FarmMitra — Procurement Sheet: {cycleLabel}`
  - Body: `Hi, please find this week's procurement sheet attached. Total orders: {n}. Cycle closes: {date}.`
- `CycleClosedEventListener.java` — listens for `CycleClosedEvent` (already published on close), calls `ProcurementEmailService` after aggregation completes
- `application.properties` — add `spring.mail.*` config (Gmail SMTP or SendGrid)

**Email service choice: Gmail SMTP**
- Free, zero additional service dependency
- Use a dedicated Gmail account (e.g. farmmitra.ops@gmail.com)
- Generate App Password (Google Account → Security → App Passwords)
- Config:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=farmmitra.ops@gmail.com
spring.mail.password=<app-password>
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Files
- New: `ProcurementEmailService.java`
- New: `CycleClosedEventListener.java` (or update existing event listener)
- `application.properties` / `application-prod.properties`
- `pom.xml` — add `spring-boot-starter-mail` dependency

---

## Implementation Order

| # | Task | Effort | Status |
|---|---|---|---|
| 1 | Add PROCUREMENT role to SecurityConfig + ProcurementController guards | XS | ✅ Done |
| 2 | Email field on user_profile (migration + model + UI) | S | ✅ Done |
| 3 | Gmail SMTP config + JavaMailSender setup | XS | ✅ Done |
| 4 | ProcurementEmailService — generate + send Excel on cycle close | M | ✅ Done |

---

## Status

| # | Task | Status |
|---|---|---|
| 1 | PROCUREMENT role | ✅ Done |
| 2 | Email field | ✅ Done |
| 3 | Mail config | ✅ Done |
| 4 | Auto email on close | ✅ Done |

---

## Async Email Flow

```
Admin closes cycle (or scheduler auto-closes)
  → CycleClosedEvent published (synchronous)
  → Returns HTTP 200 immediately to admin

[Background thread 1]
  → ProcurementService.onCycleClosed() @Async
  → aggregateForCycle() — builds procurement_sheet rows
  → autoAddStage(PROCUREMENT_STARTED)

[Background thread 2] (to be implemented)
  → CycleClosedEventListener @Async
  → ProcurementEmailService.sendExcel(cycleId)
  → fetchAllProcurementUsers() — users with ROLE_PROCUREMENT + non-null email
  → generateExcel() — reuse existing POI logic from ProcurementService
  → JavaMailSender.send() — attach Excel, send to all recipients
```

Both background threads run independently. Email sends even if aggregation is still running — Excel will have whatever data is available at that point (typically completes in < 1 second before email thread picks it up, but ordering is not guaranteed).

To guarantee aggregation completes before email: use a single `@Async` listener that aggregates first, then sends email — both in the same background thread.
