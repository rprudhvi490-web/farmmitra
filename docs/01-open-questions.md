# Open Questions

## Status Summary

| OQ   | Topic                              | Status              |
|------|------------------------------------|---------------------|
| OQ-1 | OTP provider choice                | ✅ Resolved — AWS SNS (dev mode during development, SNS for production) |
| OQ-2 | Payment gateway                    | ✅ Resolved — COD only for MVP |
| OQ-3 | Single community vs multi-community| ✅ Resolved — Single community |
| OQ-4 | Delivery slot — Sat or Sun or both | ✅ Resolved — Algorithm-assigned, see below |
| OQ-5 | Product image storage              | ✅ Resolved — Cloudinary |
| OQ-6 | Role structure — customer vs admin | ✅ Resolved — 4 roles confirmed |
| OQ-7 | Weekly cycle — auto or manual open | ✅ Resolved — Auto + admin override |
| OQ-8 | Referral rewards — points or discount | ✅ Resolved — ₹100 COD discount |
| OQ-9 | Procurement sheet export format    | ✅ Resolved — Apache POI Excel |
| OQ-10| PostgreSQL dialect config          | ✅ Resolved — see below |

---

## OQ-1 — OTP Provider Choice ✅ Resolved

**Decision:** AWS SNS for production OTP SMS delivery.
- `otp.provider=dev` during development — OTP logged to console, zero cost.
- `otp.provider=sns` for UAT with real phones and production.
- AWS SNS free tier: 100 SMS/month forever (not a trial).
- Beyond free tier: ~$0.00645/SMS (₹0.54) for India.
- SNS strategy is currently **commented out** in `AwsSnsOtpStrategy.java` to prevent accidental charges during development.
- To enable: uncomment the `send()` method body and set `otp.provider=sns`.
- Account default SMS type already set to `Transactional` in AWS SNS console — no DND filtering.

---

## OQ-2 — Payment Gateway ✅ Resolved

**Decision:** Cash on Delivery (COD) only for MVP. No payment gateway.
- `customer_order.payment_method` = COD always for now.
- `customer_order.payment_status` = PENDING until admin marks it PAID on delivery.
- Razorpay / UPI gateway deferred to Phase 2 when volume grows.
- Referral discounts applied as a manual deduction shown on the order (see OQ-8).

---

## OQ-3 — Single Community vs Multi-Community ✅ Resolved

**Decision:** Single community for MVP.
- Target community: 900+ families — sufficient scale for MVP.
- `community` table kept in schema for future expansion.
- `community_id` column on `user_profile` so multi-community can be added later without schema redesign.
- No community filtering logic needed in MVP — all queries are implicitly single-community.

---

## OQ-4 — Delivery Slot Selection ✅ Resolved

**Decision:** Algorithm-assigned delivery slots based on apartment/block proximity.
- From the customer's perspective: "Delivery between Saturday–Sunday, exact time TBD."
- Once goods arrive in Hyderabad, admin runs the delivery route optimization.
- Algorithm groups apartments by block/building proximity → assigns SAT or SUN slot to each order.
- Admin updates `customer_order.delivery_slot` in bulk after algorithm runs.
- Customer gets a push notification: "Your delivery is scheduled for Saturday."

**Delivery Algorithm (to be designed separately):**
- Input: all orders for the cycle + flat numbers + block data.
- Output: optimized delivery sequence grouped by proximity.
- Admin reviews and confirms before notifying customers.
- This is a Phase 2 feature — MVP uses manual admin assignment.

---

## OQ-5 — Product Image Storage ✅ Resolved

**Decision:** Cloudinary free tier.
- Product images and banners stored on Cloudinary.
- Backend stores the Cloudinary URL in the `product` table.
- No binary image data in DB.
- Setup steps will be provided separately when integration begins.

---

## OQ-6 — Role Structure ✅ Resolved

**Decision:** 4 roles confirmed.

| Role | Description |
|------|-------------|
| ROLE_CUSTOMER | Registered community member, places orders |
| ROLE_ADMIN | Owner/operator — manages products, orders, procurement |
| ROLE_DELIVERY | Delivery staff — marks deliveries complete |
| ROLE_SUPER_ADMIN | Platform owner — full access |

**Hierarchy:**
```
ROLE_SUPER_ADMIN > ROLE_ADMIN
ROLE_ADMIN > ROLE_DELIVERY
ROLE_ADMIN > ROLE_CUSTOMER
```

New roles can be added later via API + one line in SecurityConfig hierarchy.

---

## OQ-7 — Weekly Cycle Open/Close ✅ Resolved

**Decision:** Fully automatic with admin override capability.
- Scheduler auto-opens every Monday at 00:00.
- Scheduler auto-closes every Wednesday at 14:00.
- Admin can configure the exact open/close times via dashboard (stored in DB or properties).
- Admin can force open or force close at any time via API.

**Render free tier note:** Backend may be asleep when scheduler fires. Mitigation: use an external cron ping service (cron-job.org) to wake the backend before scheduled time. Covered in `10-notification-design.md`.

---

## OQ-8 — Referral Rewards ✅ Resolved

**Decision:** ₹100 COD discount per successful referral.
- No wallet system needed — discount applied manually at COD collection.
- When a referred user places their first order, referrer gets ₹100 credit.
- `referral.status` = `REWARDED` once the referred user's first order is DELIVERED.
- Order response includes `referral_discount` field showing how much to deduct at door.
- Admin sees referral discount on the packing/delivery list so they know to collect less.

**referral_discount on order:**
```json
{
  "orderNumber": "WB-2025-0001",
  "totalAmount": 450.00,
  "referralDiscount": 100.00,
  "amountToCollect": 350.00
}
```

---

## OQ-9 — Procurement Sheet Export Format ✅ Resolved

**Decision:** Apache POI Excel (.xlsx).
- Owner can edit quantities directly in Excel before going to market.
- Two sheets: Procurement Summary + Customer Packing List.
- Library: `poi-ooxml` dependency in `pom.xml`.
- See `09-procurement-design.md` for sheet structure.

---

## OQ-10 — PostgreSQL Dialect ✅ Resolved

**Decision:** Use `org.hibernate.dialect.PostgreSQLDialect` (Hibernate 6 auto-detects, no explicit dialect needed in Spring Boot 3.x).

```properties
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```
