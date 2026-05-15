# Community Feedback Wall — Design

## Purpose

A public-facing community wall where customers can post their experience with WeekendBasket.
Visible to all app users — builds trust, encourages new signups, and creates social proof.

---

## Business Strategy

- Positive feedback → displayed prominently, boosts community trust
- Negative feedback → admin responds publicly with a positive, solution-oriented reply
- No feedback is deleted — transparency builds credibility
- New customers see real community experiences before placing their first order

---

## Features

### Customer Side
- Post a feedback/review with a star rating (1–5)
- Edit own feedback (within 24 hours of posting)
- View all community feedback (paginated, newest first)
- Like/helpful mark on feedback posts

### Admin Side
- View all feedback in admin dashboard
- Reply to any feedback publicly
- Pin important/positive feedback to top
- Flag inappropriate content (hides from public, keeps in DB)

---

## Tables

### community_feedback

| Column       | Type          | Notes                                      |
|--------------|---------------|--------------------------------------------|
| id           | BIGSERIAL PK  |                                            |
| user_id      | BIGINT FK     | → app_user.id                              |
| rating       | INT           | 1 to 5 stars                               |
| message      | TEXT          | Customer's feedback text                   |
| is_pinned    | BOOLEAN       | default false — admin can pin to top       |
| is_flagged   | BOOLEAN       | default false — hides from public view     |
| helpful_count| INT           | default 0 — number of helpful marks        |
| + audit cols |               |                                            |

### feedback_reply

| Column       | Type          | Notes                                      |
|--------------|---------------|--------------------------------------------|
| id           | BIGSERIAL PK  |                                            |
| feedback_id  | BIGINT FK     | → community_feedback.id                   |
| replied_by   | BIGINT FK     | → app_user.id (admin who replied)          |
| message      | TEXT          | Admin's public reply                       |
| + audit cols |               |                                            |

### feedback_helpful

| Column       | Type          | Notes                                      |
|--------------|---------------|--------------------------------------------|
| id           | BIGSERIAL PK  |                                            |
| feedback_id  | BIGINT FK     | → community_feedback.id                   |
| user_id      | BIGINT FK     | → app_user.id                              |

> One row per user per feedback — prevents duplicate helpful marks.

---

## API

| Method | Path                                  | Role     | Description                        |
|--------|---------------------------------------|----------|------------------------------------|
| GET    | /feedback                             | Public   | List all feedback (paginated)      |
| POST   | /feedback                             | CUSTOMER | Post new feedback                  |
| PUT    | /feedback/{id}                        | CUSTOMER | Edit own feedback (within 24h)     |
| POST   | /feedback/{id}/helpful                | CUSTOMER | Mark feedback as helpful           |
| POST   | /feedback/{id}/reply                  | ADMIN    | Admin reply to feedback            |
| PUT    | /feedback/{id}/pin                    | ADMIN    | Pin feedback to top                |
| PUT    | /feedback/{id}/flag                   | ADMIN    | Flag inappropriate feedback        |
| GET    | /feedback/admin                       | ADMIN    | View all including flagged         |

---

## Public Feed Response Shape

```json
[
  {
    "id": 1,
    "customerName": "Ravi K.",
    "flatNumber": "A-204",
    "rating": 5,
    "message": "Fresh vegetables every week! Love the service.",
    "isPinned": true,
    "helpfulCount": 12,
    "postedAt": "2025-01-13T10:30:00",
    "adminReply": {
      "message": "Thank you Ravi! We're glad you're enjoying it. See you next week!",
      "repliedAt": "2025-01-13T11:00:00"
    }
  },
  {
    "id": 2,
    "customerName": "Priya S.",
    "flatNumber": "B-101",
    "rating": 3,
    "message": "Delivery was a bit late on Sunday.",
    "isPinned": false,
    "helpfulCount": 3,
    "postedAt": "2025-01-12T18:00:00",
    "adminReply": {
      "message": "Hi Priya, sorry for the delay! We're improving our Sunday route. Your feedback helps us get better!",
      "repliedAt": "2025-01-12T19:00:00"
    }
  }
]
```

---

## Display Rules

| Condition | Behaviour |
|-----------|-----------|
| `is_pinned = true` | Always shown at top regardless of date |
| `is_flagged = true` | Hidden from public, visible to admin only |
| Default sort | Pinned first, then newest first |
| Customer name | Show first name + last initial only (e.g. "Ravi K.") for privacy |
| Rating display | Show average rating + total count on home screen banner |

---

## Home Screen Integration

Show a summary banner on the app home screen:
```
⭐ 4.8 / 5  ·  127 reviews from your community
```

Tapping it opens the full feedback wall.

---

## Implementation Phase

- Phase 7 (after notifications and referrals)
- Tables: `community_feedback`, `feedback_reply`, `feedback_helpful`
- No complex logic — straightforward CRUD with admin moderation
