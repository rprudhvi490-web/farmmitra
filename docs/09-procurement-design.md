# Procurement Management — Design

## Purpose

Procurement is the core operational system. Instead of maintaining live inventory:
1. Collect all customer orders for the week
2. Aggregate total quantities needed per product
3. Generate a procurement sheet for bulk purchasing from Kadapa
4. Track transport stages from Kadapa to Hyderabad
5. Convert aggregated quantities back into customer-wise packing lists

---

## Three-Stage Flow

```
Stage 1 — Aggregation (auto, on cycle close)
  All order_item rows → GROUP BY product_id → procurement_sheet rows

Stage 2 — Procurement (admin-driven)
  Admin views procurement sheet → fills vendor info → marks procured
  Admin exports Excel sheet for field purchasing

Stage 3 — Transport & Packing (admin + delivery staff)
  Admin updates transport stages
  System generates packing lists per customer
  Delivery staff marks orders delivered
```

---

## Aggregation Logic

Triggered automatically when cycle closes (Wednesday 14:00):

```sql
SELECT
  oi.product_id,
  p.name,
  p.unit,
  SUM(oi.quantity) AS total_quantity
FROM order_item oi
JOIN customer_order co ON oi.order_id = co.id
JOIN product p ON oi.product_id = p.id
WHERE co.cycle_id = :cycleId
  AND co.status != 'CANCELLED'
GROUP BY oi.product_id, p.name, p.unit
ORDER BY p.name
```

Result → bulk insert into `procurement_sheet` (one row per product per cycle).

---

## procurement_sheet Table

| Column         | Notes                                    |
|----------------|------------------------------------------|
| cycle_id       | Which week                               |
| product_id     | Which product                            |
| total_quantity | Aggregated from all orders               |
| unit           | kg / piece / litre                       |
| vendor_name    | Admin fills: which supplier/market       |
| vendor_notes   | Admin fills: any notes for this product  |
| procured_qty   | Admin fills: actual quantity bought      |
| status         | PENDING → PROCURED                       |

---

## Procurement Dashboard API

`GET /api/procurement/{cycleId}` returns:

```json
{
  "cycleLabel": "Week of 12 Jan 2025",
  "totalOrders": 87,
  "items": [
    {
      "productId": 1,
      "productName": "Tomatoes",
      "unit": "kg",
      "totalQuantity": 240.0,
      "vendorName": "Kadapa Mandi",
      "procuredQty": 245.0,
      "status": "PROCURED"
    },
    {
      "productId": 2,
      "productName": "Onion",
      "unit": "kg",
      "totalQuantity": 180.0,
      "vendorName": null,
      "procuredQty": null,
      "status": "PENDING"
    }
  ]
}
```

---

## Excel Export

`GET /api/procurement/{cycleId}/export` → returns `.xlsx` file.

**Sheet 1 — Procurement Summary:**

| Product | Unit | Total Qty Needed | Vendor | Procured Qty | Status |
|---------|------|-----------------|--------|--------------|--------|
| Tomatoes | kg | 240 | Kadapa Mandi | 245 | PROCURED |
| Onion | kg | 180 | | | PENDING |

**Sheet 2 — Customer Packing List:**

| Order # | Customer | Flat | Delivery | Product | Qty |
|---------|----------|------|----------|---------|-----|
| WB-2025-0001 | Ravi Kumar | A-204 | SAT | Tomatoes | 3kg |
| WB-2025-0001 | Ravi Kumar | A-204 | SAT | Onion | 2kg |

Library: Apache POI (`poi-ooxml`).

---

## Transport Tracking

Append-only log of stages. Each stage update:
1. Admin calls `POST /api/transport/{cycleId}/stage` with stage code + notes.
2. Row inserted in `transport_tracking`.
3. `NotificationService` sends push to all customers with orders in this cycle.

**Stages in order:**
```
PROCUREMENT_STARTED  → "We've started purchasing your items in Kadapa"
GOODS_LOADED         → "Goods loaded and ready for transport"
IN_TRANSIT           → "Your groceries are on the way to Hyderabad!"
ARRIVED              → "Goods arrived in Hyderabad. Packing begins!"
PACKING              → "Your order is being packed"
DISPATCHED           → "Out for weekend delivery!"
```

Customer app "Order Tracking" screen shows these stages as a progress timeline.

---

## Packing List (Customer-wise)

`GET /api/orders/cycle/{cycleId}` with packing view returns orders grouped by delivery slot:

```json
{
  "saturday": [
    {
      "orderNumber": "WB-2025-0001",
      "customerName": "Ravi Kumar",
      "flatNumber": "A-204",
      "items": [
        { "product": "Tomatoes", "qty": "3 kg" },
        { "product": "Onion", "qty": "2 kg" }
      ]
    }
  ],
  "sunday": [ ... ]
}
```

Admin uses this to physically pack each customer's bag.

---

## Waste Reduction

Since products are bought **only after** orders are collected:
- No unsold inventory
- Procurement quantity = exactly what customers ordered (+ small buffer)
- `procured_qty` can be slightly more than `total_quantity` for buffer
- Difference tracked for future analytics

---

## Profit Optimization

- Bulk procurement from Kadapa → lower per-unit cost
- `vendor_name` tracking → compare suppliers over weeks
- Future: vendor performance analytics from `procurement_sheet` history
