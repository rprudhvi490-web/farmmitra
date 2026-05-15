# FarmMitra — Angular Web UI

## Purpose

Angular web application for FarmMitra (formerly WeekendBasket). Admin operations are web-only permanently.
Customer screens on web are a stepping stone to mobile.

---

## Why Angular Web First

- Faster iteration — no emulator or device needed
- All API contracts validated in browser before mobile
- Admin screens (procurement, delivery, transport) are better suited for web
- Mobile app reuses the same backend with zero changes

---

## Tech Stack

| Layer              | Technology              | Version   | Purpose                                      |
|--------------------|-------------------------|-----------|----------------------------------------------|
| Framework          | Angular                 | 21.x      | Component-based SPA framework                |
| Language           | TypeScript              | 5.x       | Type-safe JavaScript                         |
| UI Component Lib   | Angular Material        | 21.x      | Pre-built Material Design components         |
| Icons              | Material Icons          | latest    | Icon set bundled with Angular Material       |
| HTTP Client        | Angular HttpClient      | built-in  | API calls with interceptor support           |
| Routing            | Angular Router          | built-in  | SPA routing with lazy loading                |
| Reactive           | RxJS                    | 7.x       | Observables for async data streams           |
| Forms              | Angular Reactive Forms  | built-in  | Form validation and control                  |
| State              | Services + BehaviorSubject | —      | Lightweight state — no NgRx for MVP          |
| JWT Decode         | jwt-decode              | 4.x       | Decode JWT claims on frontend                |
| Build Tool         | Angular CLI             | 21.x      | Scaffold, build, serve                       |
| Package Manager    | npm                     | 10.x      | Dependency management                        |
| Node               | Node.js                 | 22.x      | Runtime for Angular CLI                      |
| Styles             | SCSS                    | —         | CSS preprocessor                             |
| Linting            | ESLint                  | —         | Code quality                                 |

---

## Backend Connection

| Environment | API Base URL                                          |
|-------------|-------------------------------------------------------|
| Development | `http://localhost:9090/weekendbasket/api`              |
| Production  | `https://<render-url>/weekendbasket/api`               |

Backend runs on Spring Boot 3.3.x — Java 17 — PostgreSQL (Neon).
All requests (except public endpoints) require `Authorization: Bearer <jwt>` header.

---

## Folder Structure

```
C:\WeekendBasket\UI\
├── docs\                          ← UI-specific documentation (this folder)
│   ├── README.md                  ← This file
│   ├── 01-tech-stack.md           ← Full tech stack + setup guide
│   ├── 02-api-reference.md        ← All backend APIs with request/response shapes
│   ├── 03-auth-flow.md            ← OTP login, JWT storage, guards
│   ├── 04-screens-and-routes.md   ← All screens, routes, which API each calls
│   ├── 05-modules-structure.md    ← Angular module breakdown
│   ├── 06-implementation-phases.md← Phase-by-phase build plan with status
│   ├── 07-cors-and-backend.md     ← CORS config needed on backend
│   └── 08-architecture-decisions.md ← Standalone, signals, routing, state, hosting
├── src\
│   ├── app\
│   │   ├── core\                  ← Interceptors, guards, singleton services
│   │   ├── shared\                ← Reusable components, pipes, directives
│   │   ├── auth\                  ← Login + OTP verify
│   │   ├── customer\              ← Customer-facing screens
│   │   └── admin\                 ← Admin-facing screens
│   ├── assets\
│   └── environments\
├── angular.json
├── package.json
└── proxy.conf.json                ← Dev proxy to avoid CORS in local dev
```

---

## Quick Start

```bash
# From C:\WeekendBasket\UI
npm install
ng serve
# App runs at http://localhost:4200
# API calls proxied to http://localhost:9090 via proxy.conf.json
```

---

## Role-Based Access

| Role             | Access                                      |
|------------------|---------------------------------------------|
| ROLE_CUSTOMER    | Customer screens only                       |
| ROLE_ADMIN       | Admin screens + all customer screens        |
| ROLE_SUPER_ADMIN | Everything including community management   |
| ROLE_DELIVERY    | Delivery batch screens only                 |
| Not logged in    | Redirected to /auth/login                   |

---

## Current Status

See `06-implementation-phases.md` for detailed phase tracking.
