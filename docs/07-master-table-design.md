# MasterTable — Purpose, Design & Usage

## Why MasterTable Exists

Same pattern as ElderCare — all configurable dropdown values live in one DB table.
Admin can add/update values without code changes. Frontend always fetches from API.

---

## Table Structure

| Column       | Example Value  | Purpose                                      |
|--------------|----------------|----------------------------------------------|
| id           | 1              | PK                                           |
| lookup_value | "1"            | Short key / sort order                       |
| lookup_item  | "Saturday"     | Display label shown in UI dropdown           |
| lookup_code  | "SAT"          | Code used in backend logic and DB storage    |
| type         | "DELIVERY_SLOT"| Groups related values together               |

**Rule:** `lookup_code` is unique within a `type`. Queries always filter by `type`.

---

## All Types and Their Seed Data

### ORDER_STATUS
| lookup_value | lookup_item | lookup_code |
|--------------|-------------|-------------|
| 1            | Placed      | PLACED      |
| 2            | Confirmed   | CONFIRMED   |
| 3            | Packed      | PACKED      |
| 4            | Delivered   | DELIVERED   |
| 5            | Cancelled   | CANCELLED   |

### PAYMENT_METHOD
| lookup_value | lookup_item | lookup_code |
|--------------|-------------|-------------|
| 1            | Cash on Delivery | COD    |
| 2            | UPI         | UPI         |

### PAYMENT_STATUS
| lookup_value | lookup_item | lookup_code |
|--------------|-------------|-------------|
| 1            | Pending     | PENDING     |
| 2            | Paid        | PAID        |
| 3            | Refunded    | REFUNDED    |

### DELIVERY_SLOT
| lookup_value | lookup_item | lookup_code |
|--------------|-------------|-------------|
| 1            | Saturday    | SAT         |
| 2            | Sunday      | SUN         |

### CYCLE_STATUS
| lookup_value | lookup_item   | lookup_code   |
|--------------|---------------|---------------|
| 1            | Open          | OPEN          |
| 2            | Closed        | CLOSED        |
| 3            | Procurement   | PROCUREMENT   |
| 4            | Delivering    | DELIVERING    |
| 5            | Completed     | COMPLETED     |

### TRANSPORT_STAGE
| lookup_value | lookup_item           | lookup_code           |
|--------------|-----------------------|-----------------------|
| 1            | Procurement Started   | PROCUREMENT_STARTED   |
| 2            | Goods Loaded          | GOODS_LOADED          |
| 3            | In Transit            | IN_TRANSIT            |
| 4            | Arrived in Hyderabad  | ARRIVED               |
| 5            | Packing in Progress   | PACKING               |
| 6            | Out for Delivery      | DISPATCHED            |

### NOTIFICATION_TYPE
| lookup_value | lookup_item    | lookup_code    |
|--------------|----------------|----------------|
| 1            | Order Update   | ORDER_UPDATE   |
| 2            | Announcement   | ANNOUNCEMENT   |
| 3            | Offer          | OFFER          |
| 4            | Reminder       | REMINDER       |

---

## How It Is Used

**Backend (entity field):**
```
customer_order.delivery_slot = "SAT"   ← stores lookup_code
```

**API response (enriched):**
```json
{
  "deliverySlot": "SAT",
  "deliverySlotDisplay": "Saturday"
}
```

**Frontend flow:**
1. On page load → `GET /api/masters/DELIVERY_SLOT` → populate slot dropdown
2. User selects "Saturday" → frontend sends `lookup_code = "SAT"` in request body
3. Backend stores `"SAT"` in the entity field

---

## APIs

| Method | Path                  | Role     | Description                    |
|--------|-----------------------|----------|--------------------------------|
| GET    | /api/masters/{type}   | Any auth | Get all values for a type      |
| POST   | /api/masters          | ADMIN    | Add a new lookup value         |
| PUT    | /api/masters/{id}     | ADMIN    | Update a lookup value          |
