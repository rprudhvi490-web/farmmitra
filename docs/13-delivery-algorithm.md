# Delivery Algorithm — Route Optimization Design

## Status: 🔵 Design Ready — Implementation Pending Staff Inputs

---

## Purpose

After goods arrive in Hyderabad, admin runs a delivery route optimization to:
- Group orders by apartment building
- Assign SAT or SUN delivery slot to each order
- Minimize travel distance for delivery staff
- Generate an optimized delivery sequence

---

## Supported Buildings (Greater Infra Community)

| Apartment | Location | Latitude | Longitude |
|---|---|---|---|
| Greater Infras Jasmine | HMT Swarnapuri Colony, Ameenpur | 17.5113 | 78.3374 |
| Greater Infras Iris | Greater Iris Layout, Ameenpur-Miyapur | 17.5115 | 78.3368 |
| Greater Infra's Gardenia | Greater Iris Layout, Ameenpur-Miyapur | 17.5119 | 78.3365 |
| Greater Infra Bluebells | Ameenpur Road, HMT Swarnapuri Ext. | 17.5134 | 78.3341 |
| Greater Infras Honesty | Sree Balaji Nagar, RTC Colony, Miyapur | 17.5028 | 78.3512 |
| Greater Infras Aspen | Greater Iris Layout Phase, Ameenpur | 17.5111 | 78.3371 |
| Greater Aster | Greater Iris Layout Phase, Ameenpur | 17.5108 | 78.3379 |
| Greater Infra Daffodil | Greater Iris Layout Phase, Miyapur | 17.5117 | 78.3361 |
| Greater Infra Carnation | Greater Iris Layout Phase, Ameenpur | 17.5122 | 78.3358 |

---

## Building Cluster Analysis

All 9 buildings are within ~1.5km of each other. Two natural clusters emerge from the coordinates:

### Cluster A — Core Ameenpur (7 buildings, within 400m of each other)
```
Aster       (17.5108, 78.3379)
Aspen       (17.5111, 78.3371)
Jasmine     (17.5113, 78.3374)
Iris        (17.5115, 78.3368)
Daffodil    (17.5117, 78.3361)
Gardenia    (17.5119, 78.3365)
Carnation   (17.5122, 78.3358)
```
These can all be delivered in a single walking/driving route.

### Cluster B — North Ameenpur (1 building)
```
Bluebells   (17.5134, 78.3341)  — ~400m north of Cluster A
```

### Cluster C — Miyapur Isolated (1 building)
```
Honesty     (17.5028, 78.3512)  — ~1.5km away from Cluster A
```

---

## Proposed SAT/SUN Split

**Option 1 — Proximity-based (recommended)**
- Saturday: Cluster A (7 buildings) — dense, walkable, high volume
- Sunday: Cluster B + Cluster C (2 buildings) — spread, lower volume, needs vehicle

**Option 2 — Load-balanced**
- Algorithm counts total orders per building
- Splits buildings between SAT and SUN to equalize delivery load
- Ensures neither day is overloaded

The algorithm should support both options and let admin choose.

---

## Algorithm Design

### Inputs
```
- All PLACED/CONFIRMED/PACKED orders for the cycle
- Building name from user_profile.block
- GPS coordinates from SUPPORTED_BUILDINGS map (hardcoded constant)
- delivery_date_sat, delivery_date_sun from weekly_cycle
```

### Steps

```
Step 1 — Group orders by building
  Map<buildingName, List<orderId>>

Step 2 — Assign building to cluster (A, B, C) using coordinate proximity
  For each building, find nearest cluster centroid using Haversine distance

Step 3 — Assign delivery slots
  Option 1 (proximity): Cluster A → SAT, Cluster B+C → SUN
  Option 2 (load balance): sort buildings by order count, alternate SAT/SUN

Step 4 — Within each slot, sort buildings by travel distance (nearest-neighbour)
  Start from a "depot" (admin's location or community gate)
  Greedily pick nearest unvisited building

Step 5 — Assign delivery sequence number to each order
  ORDER BY building_sequence, flat_number

Step 6 — Bulk UPDATE customer_order SET delivery_slot = 'SAT'/'SUN'

Step 7 — Create delivery_batch rows per slot per building (or per slot total)

Step 8 — Notify customers: "Your order is scheduled for Saturday/Sunday"
```

### Haversine Distance Formula (Java)
```java
double haversine(double lat1, double lon1, double lat2, double lon2) {
    double R = 6371; // Earth radius km
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat/2) * Math.sin(dLat/2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon/2) * Math.sin(dLon/2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}
```

---

## Delivery Batch Strategy

Each `delivery_batch` represents one staff member's run for one slot:

```
Batch: SAT-CLUSTER-A
  Staff: assigned delivery person
  Buildings: Jasmine, Iris, Aspen, Aster, Daffodil, Gardenia, Carnation
  Orders: sorted by building → flat number
  Status: PENDING → IN_PROGRESS → COMPLETED

Batch: SUN-OTHERS
  Staff: assigned delivery person
  Buildings: Bluebells, Honesty
  Orders: sorted by building → flat number
```

---

## API

| Method | Path | Role | Description |
|---|---|---|---|
| POST | /delivery/algorithm/{cycleId}/run | ADMIN | Run algorithm, assign slots in bulk |
| GET | /delivery/algorithm/{cycleId} | ADMIN | Preview slot assignments before save |
| POST | /delivery/algorithm/{cycleId}/confirm | ADMIN | Confirm + notify customers |

---

## Tables Involved

- `customer_order.delivery_slot` — updated by algorithm (SAT or SUN)
- `user_profile.block` — building name used as grouping key
- `delivery_batch` — one per slot assignment
- `delivery_batch_order` — maps each order to its batch

---

## Inputs Still Needed Before Implementation

- [ ] How many delivery staff are available per day?
- [ ] Max orders a single staff can deliver per day?
- [ ] Is there a depot / staging area address (start point for route)?
- [ ] Does Honesty (Miyapur) need a separate vehicle due to distance?
- [ ] Single entry point per building or multiple wings/towers?

---

## Status

- [x] Building names confirmed
- [x] GPS coordinates confirmed  
- [x] Cluster analysis done
- [x] Algorithm design documented
- [ ] Staff count / capacity inputs needed
- [ ] `DeliveryAlgorithmService.java` — not started
- [ ] Algorithm API endpoints — not started
- [ ] Admin UI for algorithm run — not started
