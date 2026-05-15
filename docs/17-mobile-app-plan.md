# Mobile Application — React Native Plan

## Status: ⏳ Planned — Starts after Angular web UI end-to-end is validated

---

## Why Mobile After Web

1. All API contracts validated on Angular web first
2. Edge cases and business logic bugs caught on web before mobile
3. Admin screens stay on web — mobile is customer-facing only
4. Faster overall delivery — web iteration is quicker than mobile builds

---

## Scope

Mobile app is **customer-facing only**.
Admin operations remain on the Angular web application.

---

## Tech Stack

| Layer         | Technology                          |
|---------------|-------------------------------------|
| Framework     | React Native (Expo managed workflow) |
| Navigation    | React Navigation v6                 |
| UI Library    | React Native Paper                  |
| HTTP          | Axios                               |
| State         | React Context + useReducer          |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Storage       | AsyncStorage (JWT token)            |
| Build         | Expo EAS Build                      |

---

## Screens (Customer Only)

| Screen | Description |
|--------|-------------|
| Login | Phone number entry |
| OTP Verify | 6-digit OTP input |
| Home | Cycle status, countdown timer, quick order CTA |
| Products | Browse by category, search |
| Product Detail | Image, price, quantity selector |
| Cart | Review items, apply referral code |
| Checkout | Confirm order + delivery slot |
| My Orders | Order history list |
| Order Detail | Items, status, tracking timeline |
| Order Tracking | Transport stage progress |
| Notifications | In-app notification list |
| Profile | View/edit name, flat, block |
| Referral | My referral code + history |
| Feedback Wall | Community reviews |

---

## FCM Push Notifications

- Register FCM token on app launch → `POST /api/fcm/register`
- Handle foreground + background notifications
- Deep link on tap:
  - `ORDER_UPDATE` → Order Detail screen
  - `ANNOUNCEMENT` → Notifications screen
  - Transport stage → Order Tracking screen

---

## API Reuse

All APIs are identical to what Angular web uses.
No backend changes needed for mobile.

---

## Implementation Phases (Mobile)

### Phase 10.1 — Setup
- [ ] Expo project setup
- [ ] Navigation structure
- [ ] Axios HTTP client + JWT interceptor
- [ ] AsyncStorage token management

### Phase 10.2 — Auth
- [ ] Login + OTP verify screens
- [ ] Role-based navigation (customer only)

### Phase 10.3 — Core Customer Flow
- [ ] Home + countdown timer
- [ ] Product browse + cart
- [ ] Checkout + place order
- [ ] My orders + order detail

### Phase 10.4 — Tracking & Notifications
- [ ] Transport tracking timeline
- [ ] FCM token registration
- [ ] Push notification handling
- [ ] In-app notifications screen

### Phase 10.5 — Polish
- [ ] Profile edit
- [ ] Referral screen
- [ ] Feedback wall
- [ ] Android + iOS testing

### Phase 10.6 — Release
- [ ] Expo EAS build (Android APK first)
- [ ] Internal testing with community members
- [ ] Play Store submission

---

## Current Status

- Phase 10: ⏳ Not Started — waiting for Angular web UI end-to-end validation
