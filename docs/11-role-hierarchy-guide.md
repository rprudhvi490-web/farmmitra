# Role Hierarchy — Where It Lives & How to Modify

## Where It Is Defined

Same pattern as ElderCare — role hierarchy lives in `SecurityConfig.java`.

```java
// SecurityConfig.java
@Bean
RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("""
        ROLE_SUPER_ADMIN > ROLE_ADMIN
        ROLE_ADMIN > ROLE_DELIVERY
        ROLE_ADMIN > ROLE_CUSTOMER
    """);
}
```

This is the **only place** you touch when changing inheritance.

---

## How to Read the Hierarchy

```
ROLE_SUPER_ADMIN > ROLE_ADMIN
```
Anyone with SUPER_ADMIN automatically has everything ADMIN has.

```
ROLE_ADMIN > ROLE_DELIVERY
ROLE_ADMIN > ROLE_CUSTOMER
```
ADMIN automatically has all DELIVERY and CUSTOMER permissions.
DELIVERY and CUSTOMER are at the same level — neither inherits from the other.

---

## Role Descriptions

| Role             | Who                  | What they can do                              |
|------------------|----------------------|-----------------------------------------------|
| ROLE_SUPER_ADMIN | Platform owner       | Everything — create communities, manage admins|
| ROLE_ADMIN       | Community owner/ops  | Products, orders, procurement, delivery mgmt  |
| ROLE_DELIVERY    | Delivery staff       | View assigned batches, mark orders delivered  |
| ROLE_CUSTOMER    | Community members    | Browse products, place orders, track delivery |

---

## Endpoint Annotation Pattern

```java
@PreAuthorize("hasRole('CUSTOMER')")   // ADMIN and SUPER_ADMIN automatically pass
public ResponseEntity<?> placeOrder(...) { }

@PreAuthorize("hasRole('ADMIN')")      // SUPER_ADMIN automatically passes
public ResponseEntity<?> getProcurementSheet(...) { }

@PreAuthorize("hasAnyRole('ADMIN', 'DELIVERY')")
public ResponseEntity<?> markDelivered(...) { }
```

---

## How to Modify — Real Scenarios

### Scenario 1: Add a VENDOR role (future)

Step 1 — Add to `role` table via API:
```json
{ "roleName": "Vendor", "roleId": "VENDOR" }
```
Service auto-prepends `ROLE_` → stored as `ROLE_VENDOR`.

Step 2 — Add to hierarchy if needed:
```java
ROLE_ADMIN > ROLE_VENDOR
```

Step 3 — Annotate vendor-specific endpoints:
```java
@PreAuthorize("hasAnyRole('ADMIN', 'VENDOR')")
```

---

### Scenario 2: Give DELIVERY staff read access to orders (already covered)

`ROLE_ADMIN > ROLE_DELIVERY` means ADMIN can see everything DELIVERY sees.
Annotate delivery endpoints with `hasRole('DELIVERY')` — ADMIN automatically passes.

---

## Role API (ADMIN only)

Service auto-prepends `ROLE_` to `roleId` input.

| Method | Path          | Role  | Description         |
|--------|---------------|-------|---------------------|
| GET    | /api/roles    | ADMIN | List all roles      |
| POST   | /api/roles    | ADMIN | Create new role     |

**POST /api/roles request:**
```json
{
  "roleName": "Vendor",
  "roleId": "VENDOR"
}
```
Stored as `ROLE_VENDOR`.

---

## Important Rule

| What | Where | Controls |
|------|-------|----------|
| Role exists | `role` table in DB | Whether a role can be assigned to users |
| Role hierarchy | `SecurityConfig.java` | Which roles inherit from which |

A role can exist in DB without being in the hierarchy — it works, just no inheritance.

---

## OTP-Only Auth Note

Unlike ElderCare (username/password), WeekendBasket uses OTP.
Admin accounts are created directly by SUPER_ADMIN — no OTP flow for admin creation.
Customer accounts are auto-created on first OTP verification.
