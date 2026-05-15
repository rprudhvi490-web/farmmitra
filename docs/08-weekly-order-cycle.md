# Weekly Order Cycle — Design

## Purpose

The weekly cycle is the heartbeat of WeekendBasket. It controls:
- When customers can place orders (ordering window)
- When procurement planning begins
- When delivery happens
- The countdown timer shown in the app

---

## Weekly Timeline

```
Monday 00:00       → Scheduler auto-creates new cycle, status = OPEN
                     Push notification: "This week's ordering is now open!"

Wednesday 14:00    → Scheduler auto-closes cycle, status = CLOSED
                     Push notification: "Ordering closed. Procurement begins!"

Wednesday Evening  → Admin reviews procurement sheet, plans bulk purchase

Thursday–Friday    → Admin updates transport stages (PROCUREMENT_STARTED → IN_TRANSIT → ARRIVED)
                     Push notifications sent on each stage update

Saturday–Sunday    → Delivery batches dispatched
                     Admin marks cycle status = DELIVERING
                     Delivery staff marks orders as DELIVERED
                     Cycle status = COMPLETED when all orders delivered
```

---

## weekly_cycle Table

| Column          | Type      | Notes                                              |
|-----------------|-----------|----------------------------------------------------|
| id              | BIGINT PK |                                                    |
| cycle_label     | VARCHAR   | e.g. "Week of 12 Jan 2025"                         |
| order_open_at   | TIMESTAMP | Monday 00:00                                       |
| order_close_at  | TIMESTAMP | Wednesday 14:00                                    |
| delivery_date_sat | DATE    | Saturday delivery date                             |
| delivery_date_sun | DATE    | Sunday delivery date                               |
| status          | VARCHAR   | OPEN / CLOSED / PROCUREMENT / DELIVERING / COMPLETED |

---

## Status Transitions

```
OPEN
  ↓ (Wednesday 14:00 — auto or admin force-close)
CLOSED
  ↓ (admin manually moves after procurement planning)
PROCUREMENT
  ↓ (admin moves when goods dispatched from Kadapa)
DELIVERING
  ↓ (auto when all orders in cycle are DELIVERED)
COMPLETED
```

Admin can force-move status at any point via `PUT /api/cycles/{id}/status`.

---

## Scheduler Logic

```java
// WeeklyOrderScheduler.java

@Scheduled(cron = "0 0 0 * * MON")   // Every Monday at midnight
public void openWeeklyCycle() {
    // Create new weekly_cycle row
    // status = OPEN
    // order_open_at = now
    // order_close_at = this Wednesday 14:00
    // delivery_date_sat = this Saturday
    // delivery_date_sun = this Sunday
    // Send broadcast push notification: ordering open
}

@Scheduled(cron = "0 0 14 * * WED")  // Every Wednesday at 14:00
public void closeWeeklyCycle() {
    // Find current OPEN cycle
    // Set status = CLOSED
    // Trigger procurement aggregation (sum all order items by product)
    // Send broadcast push notification: ordering closed
}
```

---

## Admin Override

Admin can manually open or close the cycle at any time:

```
PUT /api/cycles/{id}/open   → status = OPEN
PUT /api/cycles/{id}/close  → status = CLOSED + trigger aggregation
```

Use case: delay closing by a day if demand is low, or close early if stock is limited.

---

## Current Cycle API

`GET /api/cycles/current` returns:

```json
{
  "id": 5,
  "cycleLabel": "Week of 12 Jan 2025",
  "status": "OPEN",
  "orderOpenAt": "2025-01-13T00:00:00",
  "orderCloseAt": "2025-01-15T14:00:00",
  "deliveryDateSat": "2025-01-18",
  "deliveryDateSun": "2025-01-19",
  "timeRemainingSeconds": 172800
}
```

`timeRemainingSeconds` = `order_close_at - now` — used by the app countdown timer.
Returns `null` if no active cycle (between cycles).

---

## Ordering Validation

When a customer places an order:
1. Service calls `GET current cycle` — must be `status = OPEN`.
2. If cycle is CLOSED/null → return 400: "Ordering is currently closed. Next window opens Monday."
3. All products in order must have `available = true`.

---

## Procurement Aggregation (triggered on cycle close)

```
SELECT product_id, SUM(quantity) as total_quantity
FROM order_item oi
JOIN customer_order co ON oi.order_id = co.id
WHERE co.cycle_id = :cycleId
AND co.status != 'CANCELLED'
GROUP BY product_id
```

Result → bulk insert into `procurement_sheet` table (one row per product).

---

## App Countdown Timer

The home screen shows a countdown to `order_close_at`:
- "Order closes in 2 days 4 hours"
- When cycle is CLOSED: show current transport stage instead
- When cycle is DELIVERING: show "Your order is on the way!"
