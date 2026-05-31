# FarmMitra — Phase 9.9 Improvement Plan

## Overview
Post-deployment improvements focused on:
1. Mobile-responsive web (viewport resize)
2. Better customer UX (fewer clicks, product ratings)
3. Lightweight feel (performance + visual)
4. Backend order validation (concurrent order safety)

---

## Task 1 — Mobile Responsive Web ✅ Priority: HIGH

### Problem
App built for desktop. Mobile Chrome users see desktop layout — small text, cramped buttons, horizontal scroll.

### What to fix
- Customer layout navbar → hamburger menu on mobile
- Product grid → single column on mobile
- Cart/Checkout → full width forms
- Home screen → larger touch targets
- Admin layout → hide sidenav on mobile, show hamburger
- All tables → horizontal scroll or card view on mobile
- Buttons → minimum 44px touch target height

### Breakpoints
```
Mobile:  < 600px
Tablet:  600px - 960px
Desktop: > 960px
```

### Files to update
- `styles.scss` — global responsive utilities
- `customer-layout.component.scss` — mobile navbar
- `products.component.scss` — responsive grid
- `home.component.scss` — mobile home
- `cart.component.scss` — mobile cart
- `checkout.component.scss` — mobile checkout
- `admin-layout.component.scss` — mobile sidenav

---

## Task 2 — Reduce Customer Clicks ✅ Priority: HIGH

### Problem
Current flow: Home → Products → Browse → Add to Cart → Cart → Checkout = 5+ screens

### Proposed flow
- Home screen shows **available products directly** (no separate browse step)
- Products grouped by category with horizontal scroll chips
- "Quick Add" button on product card (adds min qty instantly)
- Cart icon shows count — tap to go directly to checkout
- One-tap reorder from My Orders (repeat last order)

### New home screen layout
```
[FarmMitra Logo]  [Cart 3]  [Profile]

Week closes in: 2d 14h 32m

━━━ Vegetables ━━━━━━━━━━━━━━━━━━━
[Tomatoes ₹40/kg ⭐4.2] [Onion ₹30/kg ⭐4.5]
[Potato ₹25/kg ⭐4.0]  [Carrot ₹50/kg ⭐4.8]

━━━ Fruits ━━━━━━━━━━━━━━━━━━━━━━━
[Banana ₹60/dz ⭐4.6]  [Apple ₹150/kg ⭐4.3]

[View All Products]
```

### Changes needed
- Merge Home + Products into one screen for customers
- Add product ratings (simple 1-5 star, admin sets)
- Quick Add button (adds minOrderQty directly)
- Reorder button on past orders

---

## Task 3 — Product Ratings ✅ Priority: MEDIUM

### Backend changes
- Add `rating DECIMAL(2,1)` column to `product` table (0.0 to 5.0)
- Admin sets rating on product edit screen
- No customer rating for MVP (admin curated)

### Frontend changes
- Show star rating on product cards
- Sort products by rating by default
- Rating badge on product image

### Migration
```sql
ALTER TABLE product ADD COLUMN rating DECIMAL(2,1) DEFAULT 0.0;
```

---

## Task 4 — Lightweight Feel ✅ Priority: MEDIUM

### Changes
- Skeleton loaders instead of spinners (perceived performance)
- Sticky "Place Order" button on checkout (always visible)
- Bottom navigation bar on mobile (Home, Shop, Cart, Orders)
- Smooth page transitions (CSS only, no animation library)
- Compress product images via Cloudinary URL params (`f_auto,q_auto`)
- Lazy load product images (`loading="lazy"`)

---

## Task 5 — Backend Order Validation (Concurrent Safety) ✅ Priority: HIGH

### Problem
Two users place orders simultaneously. No stock limit check. Both orders go through even if admin intended limited stock.

### Current behavior
- No quantity limit per product (only `minOrderQty`)
- No concurrent order check
- Two users can order same product at same time — both succeed

### What we need
For MVP (community grocery — pre-order model):
- Orders are placed BEFORE procurement — admin buys exactly what's ordered
- So technically unlimited orders are fine (admin procures what's needed)
- BUT we need to prevent:
  1. Orders after cycle closes (already done ✅)
  2. Orders for unavailable products (already done ✅)
  3. Duplicate orders — same user ordering twice in same cycle

### Duplicate order check
```java
// Already exists in OrderPlacementRule chain
// DuplicateOrderRule checks existsByUserIdAndCycleId
```

### What's actually missing
- **Re-validation at checkout time** — product availability re-checked server-side
- **Cycle status re-checked** — cycle must still be OPEN at time of DB write
- **Optimistic locking** on cycle entity to prevent race conditions

### Fix needed
Add `@Version` field to `WeeklyCycle` entity for optimistic locking.
Re-validate all order items in `OrderService.placeOrder()` at DB write time.

---

## Implementation Order

| # | Task | Effort | Status |
|---|------|--------|--------|
| 1 | Backend order validation (concurrent safety) | Small | 🔴 Not Started |
| 2 | Product ratings — DB + backend + admin UI | Small | 🔴 Not Started |
| 3 | Mobile responsive — customer screens | Medium | 🔴 Not Started |
| 4 | Reduce clicks — merge home+products | Medium | 🔴 Not Started |
| 5 | Mobile responsive — admin screens | Medium | 🔴 Not Started |
| 6 | Lightweight feel — skeleton, bottom nav | Medium | 🔴 Not Started |
| 7 | Reorder button on past orders | Small | 🔴 Not Started |

---

## Notes
- Transport auto-trigger: ✅ PROCUREMENT_STARTED auto on cycle close
- Transport auto-trigger: ✅ GOODS_LOADED auto when all items PROCURED
- Transport auto-trigger: ✅ IN_TRANSIT auto when batch created
- Transport auto-trigger: ✅ ARRIVED/PACKING/DISPATCHED auto when staff assigned
- Mark All Procured button: 🔴 Backend done, frontend pending
- Dashboard cycle history: 🔴 Pending
- Customer analytics: 🔴 Pending
