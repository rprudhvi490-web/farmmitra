# WeekendBasket — Project Overview

## What Is FarmMitra (formerly WeekendBasket)

A community-based weekly grocery aggregation platform for gated communities.
Brand name: **FarmMitra** — Fresh farm produce delivered to your community.

---

## Business Workflow

| Day                  | Activity                                      |
|----------------------|-----------------------------------------------|
| Monday               | Weekly ordering window opens                  |
| Wednesday Afternoon  | Ordering closes                               |
| Wednesday Evening    | Procurement planning (aggregate all orders)   |
| Thursday – Friday    | Bulk procurement + transportation             |
| Saturday – Sunday    | Community delivery                            |

---

## Tech Stack

| Layer            | Technology                                         |
|------------------|----------------------------------------------------|
| Web UI           | Angular (latest) — Phase 9                         |
| Mobile           | React Native (Android + iOS) — Phase 10, after web |
| UI Libraries     | Angular Material                                   |
| Backend Language | Java 17                                            |
| Framework        | Spring Boot 3.3.x                                  |
| Packaging        | JAR                                                |
| Security         | Spring Security + JWT (jjwt 0.12.x)                |
| Auth Method      | Phone number + OTP login                           |
| ORM              | Spring Data JPA + Hibernate                        |
| DB               | PostgreSQL (Neon free tier)                        |
| Backend Hosting  | Render free tier                                   |
| Notifications    | Firebase Cloud Messaging (FCM)                     |
| Image Storage    | Cloudinary free tier                               |
| CI/CD            | GitHub Actions                                     |
| Build            | Maven (JAR packaging)                              |
| Boilerplate      | Lombok + Java 17 Records (DTOs/responses)          |
| Logging          | Log4j2                                             |
| Actuator         | Spring Boot Actuator (health + endpoint listing)   |
| Source Control   | GitHub                                             |

---

## Project Structure (planned)

```
com.weekendbasket.service
├── config/
│   └── SecurityConfig.java
├── controller/
├── dto/               ← Java 17 Records for request/response
├── model/             ← JPA Entities with Lombok
├── repository/
├── service/
├── filters/
│   └── JwtFilter.java
├── utils/
│   └── JwtUtil.java
│   └── OtpUtil.java
├── exception/
│   ├── WeekendBasketException.java
│   └── GlobalExceptionHandler.java
├── scheduler/
│   └── WeeklyOrderScheduler.java
└── WeekendBasketApplication.java
```

---

## Common Audit Columns

All tables include:

| Column      | Type      | Notes                       |
|-------------|-----------|-----------------------------|
| created_by  | VARCHAR   | User who created the record |
| created_on  | TIMESTAMP | Auto-set on insert          |
| updated_on  | TIMESTAMP | Auto-set on update          |
| updated_by  | VARCHAR   | User who last modified      |

Implemented via a `BaseEntity` JPA `@MappedSuperclass`.

---

## Hosting Notes (Free Tier)

- Render free tier: backend sleeps after inactivity. Cold start = 20–60 seconds.
- Neon PostgreSQL: DB stays active even when backend sleeps.
- Suitable for MVP and small gated community usage.
- Upgrade path: Render paid tier or Railway when community scales.

---

## Community Scope

Currently serving **Greater Infra community**, Ameenpur-Miyapur, Hyderabad.

**Supported buildings:**
- Greater Infras Jasmine, Iris, Gardenia, Bluebells, Honesty, Aspen
- Greater Aster, Daffodil, Carnation

All within ~1.5km radius. Future expansion to other communities planned.

## MVP Scope

- One gated community (Greater Infra, Ameenpur)
- One weekly ordering cycle
- COD / UPI payments
- Push notifications via FCM
- Simple admin dashboard
- Focus: operational efficiency + community trust
- Building name validation on profile — only Greater Infra buildings accepted

---

## Documents

| File | Purpose |
|------|---------|
| 00-project-overview.md | This file — stack, structure, scope |
| 01-open-questions.md | Decisions pending or resolved |
| 02-auth-design.md | OTP login, JWT, role hierarchy, security config |
| 03-database-schema.md | All table definitions |
| 04-api-inventory.md | All endpoints — implemented + planned |
| 05-data-flow.md | Table-by-table data flow |
| 06-implementation-plan.md | Phased build plan |
| 07-master-table-design.md | Lookup/dropdown values |
| 08-weekly-order-cycle.md | Weekly ordering window + scheduler logic |
| 09-procurement-design.md | Aggregation, procurement sheet, transport tracking |
| 10-notification-design.md | FCM push + in-app notifications |
| 11-role-hierarchy-guide.md | Role hierarchy + SecurityConfig guide |
| 12-gap-analysis.md | ElderCare patterns vs WeekendBasket gaps |
| 13-delivery-algorithm.md | Delivery route optimization design (placeholder) |
| 14-community-feedback-wall.md | Community feedback wall + admin moderation |
| 15-cloud-deployment.md | Render + Neon + SNS + UptimeRobot deployment guide |
| 16-angular-ui-plan.md | Angular web application — phases, modules, screens |
| 17-mobile-app-plan.md | React Native mobile app — planned after web UI |
| 18-complete-order-flow.md | End-to-end order flow documentation |
| 19-phase-99-improvements.md | Phase 9.9 polish — mobile responsive, reduce clicks, ratings, concurrent safety |
| 20-phase-10-improvements.md | Phase 10 client meeting — notification banner, reorder dialog, product descriptions, user sessions |
| 21-phase-11-improvements.md | Phase 11 — banner timings, cancel order protection, stock warnings, admin cancel, mark paid guard |
| 22-phase-12-procurement-role.md | Phase 12 — PROCUREMENT role, email field, auto Excel email on cycle close |
| 23-phase-13-improvements.md | Phase 13 — procurement/transport UX fixes, community validation, building allowlist, async flow analysis |
