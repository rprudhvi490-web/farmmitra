# Delivery Algorithm — Route Optimization Design

## Status: 🔵 Placeholder — Details to be discussed

This file will be updated once apartment/block layout details are provided.

---

## Purpose

After goods arrive in Hyderabad, admin runs a delivery route optimization to:
- Group orders by apartment block / building proximity
- Assign SAT or SUN delivery slot to each order
- Minimize travel distance for delivery staff
- Generate an optimized delivery sequence

---

## From Customer Perspective

- At order placement: customer sees "Delivery between Saturday–Sunday"
- After algorithm runs: customer gets push notification — "Your delivery is scheduled for Saturday"
- `customer_order.delivery_slot` updated to SAT or SUN in bulk

---

## Inputs Needed (to be provided)

To design the algorithm, we need:

1. List of all apartment blocks/buildings in the community
2. Physical layout — which blocks are near each other
3. Entry/exit gate locations
4. Approximate number of flats per block
5. Whether one person delivers or multiple staff

---

## Planned Algorithm Approach (draft)

```
Input:
  - All orders for the cycle with flat_number + block
  - delivery_date_sat, delivery_date_sun from weekly_cycle

Step 1 — Group by block
  Orders grouped by block (Block A, Block B, Tower 1, etc.)

Step 2 — Proximity sort
  Blocks sorted by physical proximity (adjacency map to be defined)

Step 3 — Load balancing
  Split blocks between SAT and SUN to balance delivery load

Step 4 — Assign slots
  UPDATE customer_order SET delivery_slot = 'SAT' WHERE block IN (...)
  UPDATE customer_order SET delivery_slot = 'SUN' WHERE block IN (...)

Step 5 — Notify customers
  Push notification sent to each customer with their assigned slot
```

---

## API

| Method | Path                              | Role  | Description                          |
|--------|-----------------------------------|-------|--------------------------------------|
| POST   | /delivery/algorithm/{cycleId}/run | ADMIN | Run algorithm, assign slots in bulk  |
| GET    | /delivery/algorithm/{cycleId}     | ADMIN | Preview slot assignments before save |
| POST   | /delivery/algorithm/{cycleId}/confirm | ADMIN | Confirm + notify customers       |

---

## Tables Involved

- `customer_order.delivery_slot` — updated by algorithm
- `user_profile.flat_number` + `user_profile.block` — used as input
- `delivery_batch` — created after algorithm runs, grouping orders by block + slot
- `delivery_batch_order` — maps each order to its batch

---

## To Be Completed

- [ ] Receive apartment block layout from owner
- [ ] Define block adjacency / proximity map
- [ ] Decide: one delivery person or multiple
- [ ] Design load balancing logic (how many orders per day is manageable)
- [ ] Implement algorithm in `DeliveryAlgorithmService`
