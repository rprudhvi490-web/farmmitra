# Notification Design — FCM + In-App

## Purpose

Keep customers informed at every stage of the weekly cycle:
- Order placed confirmation
- Ordering window open/close
- Transport stage updates
- Delivery confirmation
- Announcements and offers

---

## Two Notification Channels

| Channel | How | Table |
|---------|-----|-------|
| Push (FCM) | Firebase Cloud Messaging → device | `fcm_token` |
| In-App | Stored in DB, fetched by app | `notification` |

Both are sent together for most events. In-app persists even if push is missed.

---

## FCM Setup

- Firebase Admin SDK added to Spring Boot backend.
- Each device registers its FCM token on app launch → `POST /api/fcm/register`.
- Token stored in `fcm_token` table linked to `user_id`.
- One user can have multiple tokens (multiple devices).
- Stale tokens (FCM returns `UNREGISTERED`) → set `active = false`.

---

## Notification Flow

```
Event occurs (e.g. transport stage updated)
  ↓
NotificationService.send(userId, title, body, type)
  ↓
  ├─→ Insert row in notification table (in-app)
  └─→ FcmService.sendPush(userId, title, body)
        → fetch all active fcm_token rows for user
        → send FCM message to each token
        → on UNREGISTERED error → mark token inactive
```

For broadcast (all customers):
```
NotificationService.broadcast(title, body, type)
  ↓
  ├─→ Insert notification row with user_id = null (broadcast)
  └─→ FcmService.sendBroadcast(title, body)
        → fetch all active fcm_token rows
        → send in batches (FCM limit: 500 per batch)
```

---

## Notification Triggers

| Event | Audience | Message |
|-------|----------|---------|
| Cycle opens (Monday) | All customers | "This week's ordering is now open! Order by Wednesday 2 PM" |
| Cycle closes (Wednesday) | All customers | "Ordering closed. We're now planning your procurement!" |
| Order placed | Customer | "Order #WB-2025-0001 placed successfully. Total: ₹450" |
| Transport: IN_TRANSIT | Customers with orders | "Your groceries are on the way from Kadapa to Hyderabad!" |
| Transport: ARRIVED | Customers with orders | "Goods arrived! Packing begins now." |
| Transport: DISPATCHED | Customers with orders | "Out for weekend delivery!" |
| Order delivered | Customer | "Your order has been delivered. Enjoy!" |
| Admin broadcast | All customers | Custom message (offers, announcements) |

---

## notification Table

| Column      | Notes                                          |
|-------------|------------------------------------------------|
| user_id     | null = broadcast (shown to all users)          |
| title       | Short notification title                       |
| body        | Full message                                   |
| type        | ORDER_UPDATE / ANNOUNCEMENT / OFFER / REMINDER |
| read_status | false until customer opens it                  |
| sent_at     | When notification was created                  |

---

## In-App Notification Screen

`GET /api/notifications/my` returns:

```json
[
  {
    "id": 10,
    "title": "Order Placed!",
    "body": "Order #WB-2025-0001 placed. Total: ₹450",
    "type": "ORDER_UPDATE",
    "readStatus": false,
    "sentAt": "2025-01-13T10:30:00"
  }
]
```

Unread count shown as badge on notification bell icon.

---

## FCM Payload Structure

```json
{
  "notification": {
    "title": "Your order is on the way!",
    "body": "Groceries dispatched from Kadapa. Weekend delivery confirmed."
  },
  "data": {
    "type": "ORDER_UPDATE",
    "cycleId": "5",
    "orderId": "101"
  }
}
```

`data` payload allows the app to deep-link to the right screen on tap.

---

## Render Free Tier Consideration

Render backend sleeps after inactivity. Scheduled notifications (Monday open, Wednesday close) are triggered by Spring `@Scheduled` — these will NOT fire if the backend is asleep.

**Solution options:**
1. Use an external cron service (cron-job.org free tier) to ping the backend before scheduled time → wakes it up.
2. Use Render paid tier (no sleep).
3. Accept the delay for MVP — admin manually triggers open/close.

**Recommendation for MVP:** Admin manually opens/closes cycle. Scheduler is a nice-to-have for Phase 2.

---

## APIs

| Method | Path                          | Role     | Description                    |
|--------|-------------------------------|----------|--------------------------------|
| GET    | /notifications/my             | Any auth | My notifications (paginated)   |
| PUT    | /notifications/{id}/read      | Any auth | Mark as read                   |
| PUT    | /notifications/read-all       | Any auth | Mark all as read               |
| POST   | /notifications/broadcast      | ADMIN    | Send broadcast to all          |
| POST   | /notifications/send           | ADMIN    | Send to specific user          |
| POST   | /fcm/register                 | Any auth | Register device FCM token      |
| DELETE | /fcm/{tokenId}                | Any auth | Remove FCM token               |
