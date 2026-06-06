# FarmMitra — Phase 13 Improvements

## Overview
Requirements from latest review session covering procurement UX fixes, community validation, transport async flow, delivery algorithm, and multi-community expansion groundwork.

---

## Issue 1 — Procurement: Cycle Selector Descending + Mark All Button Fix 🔴 Priority: HIGH

### Problem A — Cycle order
Procurement sheet cycle dropdown shows oldest cycle first. Should show latest cycle first (descending).

### Problem B — Mark All Procured button not disabling
After clicking "Mark All Procured", button is not properly disabled. The `allProcured()` check reads from the original `sheet()` signal items' `.status` field, but `editValues` map (used for inline editing) may not reflect the freshly loaded data correctly, causing a timing mismatch.

### What changes

**Procurement cycle ordering**
- Already `[...c].reverse()` in `ngOnInit` — verify the dropdown `@for` loop renders in that order ✅ (already correct per current code)
- If issue persists: confirm API returns ascending by id and reverse is working

**Mark All button disable fix**
- After `markAllProcured()` succeeds → `loadSheet()` reloads data → `sheet()` signal updates → `allProcured()` should return true → button disables
- Root fix: after reload, ensure `editValues` map is rebuilt with new statuses (already done in `loadSheet()` via `s.items.forEach`)
- Add an intermediate `markingComplete` signal that stays `true` until sheet fully reloads — prevents double-click window

**Mark All button — only for latest cycle**
- `isLatestCycle()` already guards the button via `!isLatestCycle()` disabled condition ✅
- Old cycles: export is available, but Mark All Procured is hidden — correct behaviour

### Files
- `procurement.component.ts` — add `markingComplete` guard signal
- `procurement.component.html` — wire `markingComplete` to disabled condition

---

## Issue 2 — Transport: Latest Cycle First 🔴 Priority: HIGH

### Problem
Transport tracking cycle dropdown shows oldest cycle first. Should default to latest cycle (same as procurement).

### What changes
- `transport.component.ts` — `ngOnInit` already does `[...c].reverse()` and sets `sorted[0]` as default ✅
- Verify `cycles()` signal in template renders in reversed order
- If not reversed: the `@for` in HTML iterates `cycles()` — make sure signal is set with reversed array

### Files
- `transport.component.ts` — confirm already correct (no change needed if verified)

---

## Issue 3 — Delivery Batches: Latest Cycle First 🔴 Priority: HIGH

### Problem
Batches page cycle dropdown defaults to `c[0]` which is the oldest cycle from API.

### What changes
- `batches.component.ts` `ngOnInit` — change `c[0]` → `c[c.length - 1]` (or reverse and pick `[0]`)
- Consistent with procurement and transport approach: `[...c].reverse()` then `sorted[0]`

### Files
- `batches.component.ts`

---

## Issue 4 — Community Validation: Building Names Allowlist 🔴 Priority: HIGH

### Problem
App currently focuses on one gated community (Greater Infra). When a user enters their address / building name, there's no validation. Need to auto-accept if they enter a known building name, else show popup: "We are currently supporting only Greater Infra community."

### Supported Buildings

| Apartment Name | Area |
|---|---|
| Greater Infras Jasmine | HMT Swarnapuri Colony, Ameenpur |
| Greater Infras Iris | Greater Iris Layout, Ameenpur-Miyapur |
| Greater Infra's Gardenia | Greater Iris Layout, Ameenpur-Miyapur |
| Greater Infra Bluebells | Ameenpur Road, HMT Swarnapuri Ext. |
| Greater Infras Honesty | Sree Balaji Nagar, RTC Colony, Miyapur |
| Greater Infras Aspen | Greater Iris Layout Phase, Ameenpur |
| Greater Aster | Greater Iris Layout Phase, Ameenpur |
| Greater Infra Daffodil | Greater Iris Layout Phase, Miyapur |
| Greater Infra Carnation | Greater Iris Layout Phase, Ameenpur |

### Building Coordinates (for delivery algorithm)

| Apartment | Latitude | Longitude |
|---|---|---|
| Greater Infras Jasmine | 17.5113° N | 78.3374° E |
| Greater Infras Iris | 17.5115° N | 78.3368° E |
| Greater Infra's Gardenia | 17.5119° N | 78.3365° E |
| Greater Infra Bluebells | 17.5134° N | 78.3341° E |
| Greater Infras Honesty | 17.5028° N | 78.3512° E |
| Greater Infras Aspen | 17.5111° N | 78.3371° E |
| Greater Aster | 17.5108° N | 78.3379° E |
| Greater Infra Daffodil | 17.5117° N | 78.3361° E |
| Greater Infra Carnation | 17.5122° N | 78.3358° E |

### What changes

**Frontend — Profile / Registration building input**
- Add a `SUPPORTED_BUILDINGS` constant array with all 9 building names (case-insensitive match)
- On profile save / community field blur: check if entered building name matches any in the list (fuzzy/case-insensitive contains match)
- If match found → auto-accept, proceed normally
- If no match → show Angular Material Dialog: "We currently support only Greater Infra community buildings. Please check your building name." with Close button
- Do NOT block saving — warn the user but let admin correct if needed

**Backend (optional)**
- `CommunityController` / `community` table already exists — add supported building names as community entries if not already there

### Files
- New: `shared/constants/supported-buildings.ts` — exports `SUPPORTED_BUILDINGS` array
- `customer/profile/profile.component.ts` — add validation on building/block field
- `customer/profile/profile.component.html` — trigger dialog on mismatch

---

## Issue 5 — Async Transport Auto-Progression (Mark All Procured → GOODS_LOADED) 🟡 Analysis

### Current State (already implemented)

The async flow already exists end-to-end:

```
Cycle CLOSES
  → CycleClosedEvent published
  → ProcurementService.onCycleClosed() [async, background thread]
      → aggregateForCycle()
      → transportTrackingService.autoAddStage(cycleId, "PROCUREMENT_STARTED")

Admin marks individual item PROCURED (save button)
  → ProcurementService.updateItem()
  → checks: all items PROCURED?
      → YES → transportTrackingService.autoAddStage(cycleId, "GOODS_LOADED")

Admin clicks "Mark All Procured"
  → ProcurementService.markAllProcured()
  → sets all items to PROCURED
  → transportTrackingService.autoAddStage(cycleId, "GOODS_LOADED")
```

### What's Manual (by design)

After GOODS_LOADED, the remaining stages are manual — they depend on real-world events:

| Stage | Trigger | Why Manual |
|---|---|---|
| IN_TRANSIT | Truck departs Kadapa | Physical event, someone must confirm |
| ARRIVED | Truck arrives Hyderabad | Physical event |
| PACKING | Staff starts packing | Physical event |
| DISPATCHED | Delivery starts | Physical event |

These will remain manual via the Transport Tracking page.

### Future Enhancement (not now)
- WhatsApp/SMS integration: staff sends a pre-set keyword → system auto-progresses stage
- Mobile app for delivery staff: tap button on phone → stage updates
- Keep in backlog

### No Backend Changes Needed
The async flow for PROCUREMENT_STARTED and GOODS_LOADED is already complete. The issue raised was purely UI (button not disabling — see Issue 1).

---

## Issue 6 — Procurement Email: Async on Cycle Close 🔴 Priority: MEDIUM
*(Tracked in detail in 22-phase-12-procurement-role.md)*

### Flow
```
Cycle CLOSES
  → CycleClosedEvent published
  → ProcurementService.onCycleClosed() [async] — aggregates sheet
  → CycleClosedEventListener [async] — sends Excel email to all PROCUREMENT users
```

Both listeners run asynchronously — cycle close API returns immediately (< 100ms), both tasks run on background thread pool.

### Why not block on email
- Email delivery can take 1–5 seconds
- Cycle close must be instant for admin
- `@Async` on the listener ensures non-blocking

### Email Service Choice: Gmail SMTP
- Free, zero cost, zero external service
- Use dedicated Gmail account: `farmmitra.ops@gmail.com`
- App Password from Google Account → Security → App Passwords
- Spring Boot Starter Mail already in plan

### Status: 🔴 Not Started — see 22-phase-12-procurement-role.md for implementation plan

---

## Issue 7 — Delivery Algorithm: Building Proximity Map 🔵 Planned

*(Full algorithm in 13-delivery-algorithm.md — updated separately)*

### Building Cluster Analysis (from coordinates above)

Buildings are physically close — within ~1.3km radius of each other (except Honesty which is ~1.5km away).

**Cluster A — Core Ameenpur cluster** (all within 400m of each other):
- Jasmine (17.5113, 78.3374)
- Iris (17.5115, 78.3368)
- Aspen (17.5111, 78.3371)
- Aster (17.5108, 78.3379)
- Gardenia (17.5119, 78.3365)
- Carnation (17.5122, 78.3358)
- Daffodil (17.5117, 78.3361)

**Cluster B — North Ameenpur**:
- Bluebells (17.5134, 78.3341) — ~400m from Cluster A core

**Cluster C — Miyapur (isolated)**:
- Honesty (17.5028, 78.3512) — ~1.5km from Cluster A

### Proposed Saturday/Sunday Split
- Saturday: Cluster A (7 buildings, dense, efficient single-route delivery)
- Sunday: Cluster B + Cluster C (2 buildings, spread out, smaller volume)

Or split by order count to balance load — algorithm decides.

### Algorithm Inputs Needed
- [ ] How many delivery staff are available?
- [ ] Max orders per staff per day?
- [ ] Single entry point per building or multiple floors/wings?
- [ ] Is there a staging area / community gate?

### Status: 🔵 Design ready, implementation pending inputs above

---

## Implementation Order

| # | Issue | Effort | Priority | Status |
|---|---|---|---|---|
| 1 | Batches latest cycle default | XS | HIGH | 🔴 Not Started |
| 2 | Mark All Procured button fix | XS | HIGH | 🔴 Not Started |
| 3 | Community building validation + popup | S | HIGH | 🔴 Not Started |
| 4 | Transport / procurement cycle order verify | XS | HIGH | 🔴 Not Started |
| 5 | Async transport (PROCUREMENT_STARTED + GOODS_LOADED) | ✅ | — | ✅ Already Done |
| 6 | Email on cycle close (Phase 12) | M | MEDIUM | 🔴 Not Started |
| 7 | Delivery algorithm with building coordinates | L | LOW | 🔵 Design Only |

---

## What to Take Up First

**Immediate (XS effort, unblock ops):**
1. Fix batches cycle default → `c[c.length - 1]`
2. Fix Mark All Procured button disable (add `markingComplete` signal)
3. Verify transport cycle order (likely already correct)

**Next (S effort, user-facing quality):**
4. Community building name validation + popup

**Later (M effort, automation):**
5. Phase 12 — PROCUREMENT role + email field + Gmail SMTP + auto email

**Backlog (needs more inputs):**
6. Delivery algorithm with GPS coordinates

---

## Status

| # | Task | Status |
|---|---|---|
| 1 | Batches latest cycle | ✅ Done |
| 2 | Mark All Procured button fix | ✅ Done |
| 3 | Community validation | ✅ Done |
| 4 | Async transport flow | ✅ Already working |
| 5 | Phase 12 email | ✅ Done |
| 6 | Delivery algorithm | 🔵 Design only |
