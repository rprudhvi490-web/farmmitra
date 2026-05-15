# FarmMitra — Complete Order Flow & Status Guide

## Application Name
**FarmMitra** — Fresh farm produce delivered to your community.
Previously: WeekendBasket

---

## Complete Weekly Flow (End to End)

```
MONDAY          → Admin opens cycle → Customers can order
WEDNESDAY 2PM   → Admin closes cycle → Procurement begins
WEDNESDAY EVE   → Admin reviews procurement sheet
THURSDAY-FRIDAY → Admin updates transport stages
SATURDAY-SUNDAY → Delivery batches dispatched → Orders delivered → Cash collected
```

---

## Step-by-Step Flow

### Step 1 — Admin Opens Cycle (Monday)

**Who:** Admin
**Where:** Admin → Cycles → Create New Cycle OR click Open

```
Admin creates cycle:
  - Cycle Label: "Week of 19 May 2026"
  - Order Opens: Monday 00:00
  - Order Closes: Wednesday 14:00
  - Delivery Sat: 23 May
  - Delivery Sun: 24 May

Cycle Status: OPEN
```

**What happens automatically:**
- Customers see "Ordering is open!" on home screen with countdown timer
- Notification sent to all customers: "This week's ordering is now open!"

---

### Step 2 — Admin Publishes Products

**Who:** Admin
**Where:** Admin → Products → toggle availability ON for this week's products

```
Admin enables products available for this week:
  - Tomatoes ✓
  - Onion ✓
  - Rice ✓
  (disable products not available this week)
```

**Customer sees:** Only available products in the shop

---

### Step 3 — Customer Places Order

**Who:** Customer
**Where:** Customer → Products → Cart → Checkout

```
Customer flow:
  1. Browse products by category
  2. Add to cart (select quantity)
  3. Checkout → review order → Place Order

Order created:
  - Order Number: FM-2026-0001
  - Status: PLACED
  - Payment Status: PENDING
  - Amount: ₹450
```

**Admin sees:** Order appears in Admin → Orders with status PLACED

---

### Step 4 — Admin Closes Cycle (Wednesday 2PM)

**Who:** Admin
**Where:** Admin → Cycles → click Close

```
Cycle Status: OPEN → CLOSED
```

**What happens automatically:**
- Procurement sheet generated (aggregates all order items)
- Notification: "Ordering closed. Procurement begins!"
- No new orders can be placed

---

### Step 5 — Admin Reviews Procurement Sheet

**Who:** Admin
**Where:** Admin → Procurement → select cycle

```
Admin sees aggregated quantities:
  Product      | Needed | Vendor        | Procured | Status
  Tomatoes     | 12 kg  | Kadapa Mandi  | 13 kg    | PROCURED
  Onion        | 8 kg   | Kadapa Mandi  | 8 kg     | PROCURED
  Rice         | 25 kg  | Rice Trader   | 25 kg    | PROCURED

Admin fills:
  - Vendor name for each product
  - Actual procured quantity
  - Status → PROCURED

Admin exports Excel for field purchasing
```

**Admin moves cycle:** CLOSED → PROCUREMENT

---

### Step 6 — Admin Updates Transport Stages

**Who:** Admin
**Where:** Admin → Transport → select cycle → Add Stage

```
Stages added in sequence (cannot skip):
  1. PROCUREMENT_STARTED → "Started buying at Kadapa market"
  2. GOODS_LOADED        → "Truck loaded, leaving Kadapa"
  3. IN_TRANSIT          → "On the way to Hyderabad"
  4. ARRIVED             → "Reached Hyderabad warehouse"
  5. PACKING             → "Packing individual orders"
  6. DISPATCHED          → "Out for weekend delivery"
```

**Customer sees:** Transport timeline on their order detail screen
**Notification sent** to all customers at each stage

---

### Step 7 — Admin Creates Delivery Batches

**Who:** Admin
**Where:** Admin → Delivery → New Batch

```
Admin groups orders by location:
  Batch 1: "Block A — Saturday"
    - Orders: FM-2026-0001, FM-2026-0002
    - Staff: Ravi (delivery person)
    - Date: 23 May

  Batch 2: "Block B — Sunday"
    - Orders: FM-2026-0003, FM-2026-0004
    - Staff: Kumar
    - Date: 24 May
```

**Admin moves cycle:** PROCUREMENT → DELIVERING

---

### Step 8 — Delivery Staff Marks Orders Delivered

**Who:** Admin / Delivery Staff
**Where:** Admin → Delivery → expand batch → click "Mark Delivered"

```
For each order in batch:
  Delivery staff goes to customer door
  Collects cash
  Clicks "Mark Delivered"

Order Status: PLACED → CONFIRMED → PACKED → DELIVERED (auto-advanced)
```

**Customer receives notification:** "Your order has been delivered. Enjoy!"

---

### Step 9 — Admin Marks Payment as Paid

**Who:** Admin
**Where:** Admin → Orders → click "Mark Paid"

```
After collecting cash at door:
  Payment Status: PENDING → PAID

Admin sees:
  Order FM-2026-0001 | DELIVERED | ✓ PAID | ₹450
```

---

### Step 10 — Cycle Completed

**Who:** Admin
**Where:** Admin → Cycles → click "→ Complete"

```
Cycle Status: DELIVERING → COMPLETED
```

---

## Status Reference

### Order Status Flow
```
PLACED → CONFIRMED → PACKED → DELIVERED
PLACED → CANCELLED (customer cancels while cycle OPEN)
CONFIRMED → CANCELLED (admin cancels)
```

| Status | Meaning | Who Sets |
|--------|---------|----------|
| PLACED | Customer placed order | Auto on checkout |
| CONFIRMED | Admin reviewed | Admin via Orders screen |
| PACKED | Physically packed | Admin via Orders screen |
| DELIVERED | Handed to customer | Delivery staff via Batches |
| CANCELLED | Order cancelled | Customer (PLACED only) or Admin |

### Payment Status Flow
```
PENDING → PAID
```

| Status | Meaning | Who Sets |
|--------|---------|----------|
| PENDING | Cash not yet collected | Default on order |
| PAID | Cash collected at door | Admin via Orders → Mark Paid |

### Cycle Status Flow
```
OPEN → CLOSED → PROCUREMENT → DELIVERING → COMPLETED
CLOSED → OPEN (admin re-opens if needed)
```

| Status | Meaning |
|--------|---------|
| OPEN | Customers can place orders |
| CLOSED | Ordering closed, procurement aggregated |
| PROCUREMENT | Buying goods from Kadapa |
| DELIVERING | Weekend delivery in progress |
| COMPLETED | All orders delivered |

### Transport Stage Flow (Sequential — cannot skip)
```
PROCUREMENT_STARTED → GOODS_LOADED → IN_TRANSIT → ARRIVED → PACKING → DISPATCHED
```

### Batch Status Flow
```
PENDING → IN_PROGRESS → DONE
```

---

## Revenue & Payment Tracking

### How Admin Sees Revenue

**Admin → Dashboard:**
- Total orders this cycle
- Total revenue this cycle (sum of all non-cancelled orders)
- Active products count

**Admin → Orders (filter by cycle):**
- Each order shows: Amount, Payment Status (PENDING/PAID)
- Mark Paid button for cash collection

### Revenue Breakdown Per Cycle

| View | Where |
|------|-------|
| Total orders this cycle | Dashboard → Orders This Cycle |
| Total revenue | Dashboard → Revenue This Cycle |
| Per-order payment status | Admin → Orders → Payment column |
| Unpaid orders | Admin → Orders → filter PENDING payment |

### How to See Previous Cycles

**Admin → Cycles:**
- All cycles listed with status
- Click cycle in Orders dropdown to see that cycle's orders

**Admin → Procurement:**
- Select any past cycle from dropdown
- See what was procured, vendor details, quantities

**Admin → Transport:**
- Select any past cycle
- See full transport stage log with timestamps

---

## Features Pending / Roadmap

### Phase 9.9 — Polish (In Progress)
- [x] FarmMitra brand theme — Nunito font, CSS design tokens, consistent across all screens
- [x] Admin broadcast notifications UI
- [x] Dashboard — cycle history (revenue generated vs collected vs pending)
- [x] Dashboard — customer loyalty analytics (NEW / REGULAR / LOYAL / CHAMPION)
- [ ] Responsive mobile layout
- [ ] Production deployment (Netlify + Render)

### Phase 10 — Mobile App (React Native)
- Customer-facing screens only
- Same backend APIs
- FCM push notifications
- Android APK first

---

## Key Business Rules

| Rule | Detail |
|------|--------|
| Order window | Monday 00:00 to Wednesday 14:00 |
| Min order qty | Per product (e.g. 0.5 kg for tomatoes) |
| Payment | COD only for MVP |
| Referral reward | ₹100 discount on next order |
| OTP expiry | 5 minutes, max 3 attempts |
| JWT expiry | 10 hours (OTP login), 7 days (password login) |
| Delivery slots | Saturday or Sunday (assigned by admin) |
| Procurement | Auto-generated when cycle closes |
| Transport stages | Sequential — cannot skip stages |
| Batch delivery | Auto-advances order through CONFIRMED → PACKED → DELIVERED |
