# Angular Architecture Decisions

Angular CLI: 21.2.11 | Node: 22.14.0 | Angular 22 RC exists but we use 21 (stable)

---

## 1. NgModule vs Standalone Components

**Decision: Standalone Components throughout — no NgModule except AppModule root.**

Angular 14+ introduced standalone components. Angular 17+ made them the default.
In Angular 21, standalone is the recommended approach.

What this means:
- Every component, directive, pipe has `standalone: true`
- Each component declares its own imports (no shared NgModule barrel)
- `AppModule` still bootstraps the app via `bootstrapApplication()`
- Lazy loaded routes use `loadComponent()` instead of `loadChildren()`

```typescript
// Every component looks like this
@Component({
  standalone: true,
  imports: [MatCardModule, CommonModule, RouterLink],
  template: `...`
})
export class ProductListComponent {}
```

Why not NgModule:
- Less boilerplate — no module file per feature
- Tree shaking is more precise
- Easier to reason about what each component needs
- Angular 21 CLI generates standalone by default

---

## 2. Bootstrapping — platformBrowser vs platformBrowserDynamic

**Decision: `platformBrowserDynamic` is NOT used. We use `bootstrapApplication()` directly.**

`platformBrowserDynamic` was used with NgModule-based apps (`bootstrapModule(AppModule)`).
With standalone components, Angular 21 uses:

```typescript
// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';

bootstrapApplication(AppComponent, appConfig);
```

`appConfig` is where providers are registered (router, HTTP, animations).

`platformBrowser` is used internally by `bootstrapApplication` — you never call it directly.

---

## 3. Routing & Deep Linking

**Decision: Angular Router with `withComponentInputBinding()` + hash-free URLs + lazy `loadComponent()`.**

### Route Structure

```
/                         → redirect to /auth/login
/auth/login               → LoginComponent (standalone, public)
/auth/verify              → VerifyOtpComponent (standalone, public)
/customer                 → CustomerShellComponent (lazy)
  /customer/home
  /customer/products
  /customer/products/:id
  /customer/cart
  /customer/checkout
  /customer/orders
  /customer/orders/:id
  /customer/notifications
  /customer/profile
  /customer/referral
  /customer/feedback
/admin                    → AdminShellComponent (lazy)
  /admin/dashboard
  /admin/cycles
  /admin/products
  /admin/categories
  /admin/orders
  /admin/orders/:id
  /admin/procurement/:cycleId
  /admin/transport/:cycleId
  /admin/batches/:cycleId
  /admin/batches/:id/orders
  /admin/users
  /admin/notifications
  /admin/feedback
  /admin/masters
  /admin/communities
```

### Deep Linking

Angular Router uses HTML5 `pushState` by default — URLs look like `/customer/home` not `/#/customer/home`.

For deep linking to work on hosting (Netlify/Firebase):
- All 404s must redirect to `index.html`
- Netlify: `_redirects` file with `/* /index.html 200`
- Firebase Hosting: `rewrites` in `firebase.json`

Route params accessed via `input()` binding (Angular 16+ feature, enabled by `withComponentInputBinding()`):
```typescript
// Instead of injecting ActivatedRoute
@Input() cycleId!: string;  // auto-bound from route param :cycleId
```

### Guards

```typescript
// auth.guard.ts
export const authGuard: CanActivateFn = () => {
  const token = inject(TokenService);
  const router = inject(Router);
  if (token.isLoggedIn()) return true;
  return router.createUrlTree(['/auth/login']);
};

// role.guard.ts
export const roleGuard: CanActivateFn = (route) => {
  const token = inject(TokenService);
  const router = inject(Router);
  const required: string[] = route.data['roles'];
  const has = token.getRoles().some(r => required.includes(r));
  if (has) return true;
  return router.createUrlTree(['/customer/home']);
};
```

Guards are functional (not class-based) — Angular 15+ pattern, no need to inject or provide them.

---

## 4. State Management

**Decision: No NgRx. Services with `signal()` for local state, `BehaviorSubject` for shared streams.**

Angular 21 has Signals as stable. We use them for component-level reactive state.
`BehaviorSubject` for cross-component shared state (cart, auth state).

### Auth State

```typescript
// token.service.ts — singleton, provided in root
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly KEY = 'wb_token';

  save(token: string) { localStorage.setItem(this.KEY, token); }
  get(): string | null { return localStorage.getItem(this.KEY); }
  remove() { localStorage.removeItem(this.KEY); }
  getRoles(): string[] { /* decode JWT */ }
  isLoggedIn(): boolean { return !!this.get() && !this.isExpired(); }
  isExpired(): boolean { /* check exp claim */ }
}
```

### Cart State (BehaviorSubject)

```typescript
// cart.service.ts
@Injectable({ providedIn: 'root' })
export class CartService {
  private items$ = new BehaviorSubject<CartItem[]>([]);

  readonly items = this.items$.asObservable();
  readonly count = computed(() => ...);  // or use toSignal()

  add(item: CartItem) { ... }
  remove(productId: number) { ... }
  clear() { this.items$.next([]); }
  getTotal(): number { ... }
}
```

### Component-Level State (Signals)

```typescript
// product-list.component.ts
export class ProductListComponent {
  products = signal<Product[]>([]);
  loading = signal(false);
  selectedCategory = signal<number | null>(null);

  // Derived state
  filteredProducts = computed(() =>
    this.selectedCategory()
      ? this.products().filter(p => p.categoryId === this.selectedCategory())
      : this.products()
  );
}
```

### Role-Based UI State

Role is decoded from JWT once on login and stored in `TokenService`.
Components check roles via `TokenService.getRoles()` or `isAdmin()` helper.

```html
<!-- Show only for admin -->
@if (tokenService.isAdmin()) {
  <button mat-button routerLink="/admin/dashboard">Admin Panel</button>
}
```

No separate role store needed — JWT is the source of truth.

---

## 5. HTTP & Observables

**Decision: `HttpClient` with functional interceptors. `takeUntilDestroyed()` for unsubscription.**

### HTTP Interceptors (Functional — Angular 15+)

```typescript
// auth.interceptor.ts
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(TokenService).get();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};

// error.interceptor.ts
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const snackbar = inject(MatSnackBar);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) { /* clear token, redirect */ }
      if (err.status === 400) { snackbar.open(err.error?.message, 'Close', { duration: 3000 }); }
      return throwError(() => err);
    })
  );
};
```

Registered in `appConfig`:
```typescript
provideHttpClient(withInterceptors([authInterceptor, errorInterceptor]))
```

### Observable Unsubscription

**We use `takeUntilDestroyed()` — Angular 16+ built-in, no manual Subject needed.**

```typescript
export class ProductListComponent {
  private destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.productService.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(products => this.products.set(products));
  }
}
```

No `ngOnDestroy()`, no `Subject + takeUntil()` pattern needed.

For one-shot HTTP calls (POST, PUT, DELETE) — no unsubscription needed, HTTP completes automatically.

### Promises vs Observables

**We use Observables throughout. No `.toPromise()` or `async/await` on HTTP calls.**

Exception: `async/await` is fine inside guards and resolvers where Angular expects a Promise or Observable.

```typescript
// Guards return Observable<boolean | UrlTree> — fine
// HTTP calls return Observable<T> — always subscribe, never toPromise
```

---

## 6. CSS Strategy

**Decision: Angular Material + component-scoped SCSS. No Tailwind, no Bootstrap.**

### Why Angular Material

- Consistent Material Design out of the box
- Accessibility built in (ARIA, keyboard nav)
- Theming system via SCSS tokens
- All components we need exist: table, form, dialog, snackbar, sidenav, chips

### SCSS Structure

```
src/
├── styles.scss              ← Global: Material theme, body reset, utility classes
├── _variables.scss          ← Brand colors, spacing tokens
└── app/
    └── each component has its own .scss file (ViewEncapsulation.Emulated default)
```

### Global Theme (`styles.scss`)

```scss
@use '@angular/material' as mat;

$wb-primary: mat.define-palette(mat.$green-palette, 700);
$wb-accent:  mat.define-palette(mat.$orange-palette, 600);
$wb-theme:   mat.define-light-theme((
  color: (primary: $wb-primary, accent: $wb-accent)
));

@include mat.all-component-themes($wb-theme);

body { margin: 0; font-family: Roboto, sans-serif; }
```

### Component SCSS

Each component's `.scss` is scoped — no class name collisions.
Use Angular Material layout utilities (`fxLayout` is deprecated — use CSS Grid/Flexbox directly).

```scss
// product-list.component.scss
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
  padding: 16px;
}
```

### Responsive Breakpoints

```scss
// _variables.scss
$breakpoint-mobile:  600px;
$breakpoint-tablet:  960px;
$breakpoint-desktop: 1280px;
```

Use Angular CDK `BreakpointObserver` for JS-side responsive logic (e.g. collapse sidenav on mobile).

---

## 7. Screen Layout Strategy

**No Figma/wireframes — we derive screens from API responses and business flow.**

### How Each Screen Is Designed

1. Look at what data the API returns (see `02-api-reference.md`)
2. Identify the primary action on that screen
3. Use the simplest Material component that fits:
   - List of items → `MatTable` or `MatCard` grid
   - Form → `MatFormField` + `MatInput` + `MatSelect`
   - Status → `MatChip` with color
   - Action → `MatButton` or `MatIconButton`
   - Confirmation → `MatDialog`
   - Feedback → `MatSnackBar`

### Customer Layout

```
┌─────────────────────────────────────────┐
│  MatToolbar: Logo | [Nav Links] | 🔔 👤  │
├─────────────────────────────────────────┤
│                                         │
│         <router-outlet>                 │
│                                         │
└─────────────────────────────────────────┘
```

### Admin Layout

```
┌──────────┬──────────────────────────────┐
│          │  MatToolbar: ☰ | Title | 👤  │
│ MatSide  ├──────────────────────────────┤
│   nav    │                              │
│          │      <router-outlet>         │
│ (links)  │                              │
└──────────┴──────────────────────────────┘
```

Sidenav collapses to icon-only on tablet, hidden on mobile (hamburger menu).

---

## 8. Web Workers

**Decision: Not needed for MVP.**

Web Workers are for CPU-intensive tasks (image processing, large data crunching).
Our app is API-driven — all heavy computation is on the backend.
No Web Workers needed.

---

## 9. Dependency Injection

**Decision: `providedIn: 'root'` for all services. No module-level providers.**

```typescript
@Injectable({ providedIn: 'root' })
export class ProductService { ... }
```

- Single instance across the app
- Tree-shakeable — if not used, not bundled
- No need to add to any `providers` array

Exception: `MatDialog` data injection uses `MAT_DIALOG_DATA` token — that's Angular Material's pattern, not custom.

---

## 10. Hosting — Free Tier

**Decision: Netlify (free tier) for Angular web app.**

| Service | What | Cost |
|---------|------|------|
| Netlify | Angular web app hosting | Free (100GB bandwidth/month) |
| Render | Spring Boot backend | Free (existing) |
| Neon | PostgreSQL | Free (existing) |

### Why Netlify over Firebase Hosting

- No Google account coupling
- Simpler deploy: drag-and-drop or GitHub auto-deploy
- `_redirects` file handles Angular deep linking in one line
- Free SSL, custom domain support

### Deploy Steps

```bash
# Build production bundle
ng build --configuration production
# Output: dist/weekendbasket-ui/browser/

# _redirects file (place in src/assets/ or public/)
/* /index.html 200
```

1. Push `UI/` folder to GitHub
2. Connect repo to Netlify
3. Build command: `ng build --configuration production`
4. Publish directory: `dist/weekendbasket-ui/browser`
5. Netlify auto-deploys on every push to `main`

### Environment Variable for Production API URL

In Netlify dashboard → Site settings → Environment variables:
```
ANGULAR_PROD_API_URL = https://<render-url>/weekendbasket/api
```

Or just hardcode in `environment.prod.ts` since it's not a secret.

---

## 11. Summary Table

| Concern | Decision | Why |
|---------|----------|-----|
| Component style | Standalone | Angular 21 default, less boilerplate |
| Bootstrap | `bootstrapApplication()` | Standalone apps don't use NgModule bootstrap |
| Routing | Functional guards + `loadComponent()` | Modern Angular pattern |
| Deep linking | HTML5 pushState + Netlify `_redirects` | Clean URLs |
| Route params | `withComponentInputBinding()` + `@Input()` | No ActivatedRoute injection needed |
| State | Signals (local) + BehaviorSubject (shared) | Right tool for each scope |
| HTTP | `HttpClient` + functional interceptors | Angular 21 recommended |
| Unsubscription | `takeUntilDestroyed()` | No manual cleanup needed |
| Promises | Avoided — Observables only | Consistent async pattern |
| CSS | Angular Material + component SCSS | Consistent, accessible |
| Responsive | CSS Grid/Flex + CDK BreakpointObserver | No extra library |
| DI | `providedIn: 'root'` everywhere | Simple, tree-shakeable |
| Web Workers | Not used | No CPU-heavy work on frontend |
| Hosting | Netlify free tier | Simple, deep-link friendly |
