# Tech Stack & Setup Guide

## Prerequisites

| Tool        | Version  | Check Command       | Install                              |
|-------------|----------|---------------------|--------------------------------------|
| Node.js     | 22.x     | `node --version`    | Already installed                    |
| npm         | 10.x     | `npm --version`     | Comes with Node                      |
| Angular CLI | 21.x     | `ng version`        | Already installed globally           |

---

## Dependencies

### Core Angular (installed by `ng new`)

| Package                    | Purpose                              |
|----------------------------|--------------------------------------|
| `@angular/core`            | Angular framework                    |
| `@angular/common`          | Common directives, pipes, HttpClient |
| `@angular/router`          | SPA routing                          |
| `@angular/forms`           | Reactive forms                       |
| `@angular/platform-browser`| Browser rendering                    |
| `rxjs`                     | Reactive streams                     |
| `typescript`               | Type-safe JS                         |
| `zone.js`                  | Change detection                     |

### Angular Material (installed by `ng add @angular/material`)

| Package                    | Purpose                              |
|----------------------------|--------------------------------------|
| `@angular/material`        | UI components (buttons, tables, etc) |
| `@angular/cdk`             | Component Dev Kit (overlays, etc)    |
| `@angular/animations`      | Material animation support           |

### Additional Packages (install manually)

| Package       | Version | Install Command              | Purpose                          |
|---------------|---------|------------------------------|----------------------------------|
| `jwt-decode`  | ^4.0.0  | `npm install jwt-decode`     | Decode JWT claims (roles, phone) |

---

## Angular Material Components Used

| Component              | Used In                                      |
|------------------------|----------------------------------------------|
| `MatToolbar`           | Top navigation bar                           |
| `MatSidenav`           | Admin sidebar navigation                     |
| `MatCard`              | Product cards, order cards                   |
| `MatTable`             | Admin order list, procurement sheet          |
| `MatPaginator`         | Paginated tables                             |
| `MatFormField`         | All form inputs                              |
| `MatInput`             | Text inputs (phone, OTP, search)             |
| `MatButton`            | All buttons                                  |
| `MatIcon`              | Icons throughout                             |
| `MatSnackBar`          | Toast notifications (success/error)          |
| `MatDialog`            | Confirm dialogs                              |
| `MatChips`             | Status badges (order status, cycle status)   |
| `MatProgressSpinner`   | Loading states                               |
| `MatSelect`            | Dropdowns (category filter, status update)   |
| `MatBadge`             | Notification bell unread count               |
| `MatStepper`           | OTP login 2-step flow                        |
| `MatMenu`              | User profile dropdown                        |
| `MatDivider`           | Section separators                           |
| `MatTooltip`           | Hover hints                                  |

---

## Project Setup Commands

```bash
# Step 1 — Angular workspace already scaffolded
# If starting fresh:
cd C:\WeekendBasket\UI
ng new . --routing --style=scss --skip-git --skip-tests

# Step 2 — Add Angular Material
ng add @angular/material
# Choose: Indigo/Pink theme (or custom), Yes to typography, Yes to animations

# Step 3 — Install jwt-decode
npm install jwt-decode

# Step 4 — Serve with proxy
ng serve --proxy-config proxy.conf.json
```

---

## proxy.conf.json

Place in `C:\WeekendBasket\UI\proxy.conf.json`:

```json
{
  "/weekendbasket": {
    "target": "http://localhost:9090",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
}
```

With this proxy, Angular dev server forwards all `/weekendbasket/**` calls to the Spring Boot backend.
No CORS issues during local development.

---

## Environment Files

### `src/environments/environment.ts` (development)

```typescript
export const environment = {
  production: false,
  apiBaseUrl: '/weekendbasket/api'
};
```

Using relative path `/weekendbasket/api` so the proxy handles routing to `localhost:9090`.

### `src/environments/environment.prod.ts` (production)

```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://<your-render-url>/weekendbasket/api'
};
```

---

## angular.json — Proxy Config Registration

In `angular.json` under `projects > ui > architect > serve > options`:

```json
"proxyConfig": "proxy.conf.json"
```

---

## TypeScript Config Notes

`tsconfig.json` — ensure these compiler options:

```json
{
  "compilerOptions": {
    "strict": true,
    "strictNullChecks": true,
    "noImplicitAny": true
  }
}
```

---

## SCSS Global Styles

`src/styles.scss`:

```scss
@use '@angular/material' as mat;

html, body {
  height: 100%;
  margin: 0;
  font-family: Roboto, "Helvetica Neue", sans-serif;
}
```

---

## VS Code Extensions Recommended

| Extension                  | Purpose                        |
|----------------------------|--------------------------------|
| Angular Language Service   | Template autocomplete          |
| ESLint                     | Linting                        |
| Prettier                   | Code formatting                |
| Material Icon Theme        | File icons                     |
