# FarmMitra — Phase 14 Improvements
## Complete Implementation Plan

Each task is self-contained. Do one per session. Mark ✅ when done.

---

## Task 1 — Notifications: Send by Phone Number (not User ID)
**Status:** 🔴 Not Started
**Effort:** XS — 3 file changes

### Why
Admin doesn't know user IDs. They see phone numbers on the Users tab. Sending by phone is natural.

### Backend Changes

**File: `backend/src/main/java/com/weekendbasket/app/dto/NotificationDto.java`**

Replace `SendNotificationRequest` record:
```java
// BEFORE
public record SendNotificationRequest(
    @NotNull(message = "User ID is required") Long userId,
    @NotBlank String title,
    @NotBlank String body,
    @NotBlank String type
) {}

// AFTER
public record SendNotificationRequest(
    @NotBlank(message = "Phone number is required") String phoneNumber,
    @NotBlank String title,
    @NotBlank String body,
    @NotBlank String type
) {}
```

**File: `backend/src/main/java/com/weekendbasket/app/controller/NotificationController.java`**

Replace `sendToUser` method:
```java
// BEFORE
@PostMapping("/send")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> sendToUser(@Valid @RequestBody SendNotificationRequest request) {
    notificationService.sendToUser(request.userId(), request.title(), request.body(), request.type());
    return ResponseEntity.noContent().build();
}

// AFTER
@PostMapping("/send")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> sendToUser(@Valid @RequestBody SendNotificationRequest request) {
    User user = userRepository.findByPhoneNumber(request.phoneNumber())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.phoneNumber()));
    notificationService.sendToUser(user.getId(), request.title(), request.body(), request.type());
    return ResponseEntity.noContent().build();
}
```
Also add import: `import com.weekendbasket.app.model.User;`

### Frontend Changes

**File: `UI/src/app/admin/services/admin.services.ts`**

In `NotificationAdminService.sendToUser()`:
```typescript
// BEFORE
sendToUser(req: { userId: number | null; title: string; body: string; type: string }): Observable<any> {
    return this.http.post(`${this.base}/notifications/send`, req);
}

// AFTER
sendToUser(req: { phoneNumber: string; title: string; body: string; type: string }): Observable<any> {
    return this.http.post(`${this.base}/notifications/send`, req);
}
```

**File: `UI/src/app/admin/notifications/notifications.component.ts`**

```typescript
// BEFORE
direct = { userId: null as number | null, title: '', body: '', type: 'ORDER_UPDATE' };

// AFTER
direct = { phoneNumber: '', title: '', body: '', type: 'ORDER_UPDATE' };
```

In `sendDirect()`:
```typescript
// BEFORE
if (!this.direct.userId || !this.direct.title || !this.direct.body) return;

// AFTER
if (!this.direct.phoneNumber || !this.direct.title || !this.direct.body) return;
```

On success reset:
```typescript
// BEFORE
this.direct = { userId: null, title: '', body: '', type: 'ORDER_UPDATE' };

// AFTER
this.direct = { phoneNumber: '', title: '', body: '', type: 'ORDER_UPDATE' };
```

**File: `UI/src/app/admin/notifications/notifications.component.html`**

```html
<!-- BEFORE -->
<mat-form-field appearance="outline">
  <mat-label>User ID</mat-label>
  <input matInput type="number" [(ngModel)]="direct.userId" placeholder="e.g. 42" />
</mat-form-field>

<!-- AFTER -->
<mat-form-field appearance="outline">
  <mat-label>Phone Number</mat-label>
  <input matInput type="tel" [(ngModel)]="direct.phoneNumber" placeholder="e.g. 9876543210" />
</mat-form-field>
```

Also update disabled condition:
```html
<!-- BEFORE -->
[disabled]="directSending() || !direct.userId || !direct.title || !direct.body"

<!-- AFTER -->
[disabled]="directSending() || !direct.phoneNumber || !direct.title || !direct.body"
```

### Test
1. Go to Admin → Notifications → Send to User tab
2. Enter phone number `6305262393`, title "Test", body "Hello"
3. Check customer's notifications — should receive it

---

## Task 2 — Super Admin: Create New User
**Status:** 🔴 Not Started
**Effort:** S — 5 file changes

### Why
Super admin needs to create accounts for delivery staff, admins, or specific customers manually.

### Backend Changes

**File: `backend/src/main/java/com/weekendbasket/app/dto/UserDto.java`**

Add new record at the end of the class:
```java
public record CreateUserRequest(
    @NotBlank(message = "Phone number is required") String phoneNumber,
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Role ID is required") String roleId
) {}
```

**File: `backend/src/main/java/com/weekendbasket/app/service/UserService.java`**

Add method:
```java
@Transactional
public UserResponse createUser(CreateUserRequest req) {
    if (userRepository.existsByPhoneNumber(req.phoneNumber())) {
        throw new WeekendBasketException("User with this phone number already exists.");
    }
    User user = User.builder()
            .phoneNumber(req.phoneNumber())
            .password("N/A")
            .username(req.username())
            .status("ACTIVE")
            .hasPassword(false)
            .build();
    userRepository.save(user);

    UserProfile profile = UserProfile.builder().user(user).build();
    userProfileRepository.save(profile);

    Role role = roleRepository.findByRoleId(req.roleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + req.roleId()));
    roleAccessRepository.save(RoleAccess.builder().user(user).role(role).build());

    return toResponse(user);
}
```

Add required imports and inject `roleRepository`, `roleAccessRepository`, `userProfileRepository` if not already present.

**File: `backend/src/main/java/com/weekendbasket/app/controller/UserController.java`**

Add endpoint:
```java
@PostMapping("/create")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.ok(userService.createUser(request));
}
```

Add import: `import com.weekendbasket.app.dto.UserDto.CreateUserRequest;`

### Frontend Changes

**File: `UI/src/app/admin/services/admin.services.ts`**

Add to `AdminUserService`:
```typescript
create(req: { phoneNumber: string; username: string; roleId: string }): Observable<any> {
    return this.http.post(`${this.base}/users/create`, req);
}
```

**File: `UI/src/app/admin/users/users.component.ts`**

Add signals and method:
```typescript
import { TokenService } from '../../core/services/token.service';

// Add to class
private tokenService = inject(TokenService);
showCreateForm = signal(false);
creating = signal(false);
createForm = { phoneNumber: '', username: '', roleId: 'ROLE_CUSTOMER' };
isSuperAdmin = this.tokenService.hasRole('ROLE_SUPER_ADMIN');
roleOptions = ['ROLE_CUSTOMER', 'ROLE_ADMIN', 'ROLE_DELIVERY', 'ROLE_SUPER_ADMIN'];

createUser(): void {
    if (!this.createForm.phoneNumber || !this.createForm.username) return;
    this.creating.set(true);
    this.userService.create(this.createForm).subscribe({
        next: () => {
            this.snackbar.open('User created!', 'Close', { duration: 2000 });
            this.creating.set(false);
            this.showCreateForm.set(false);
            this.createForm = { phoneNumber: '', username: '', roleId: 'ROLE_CUSTOMER' };
            this.load();
        },
        error: () => this.creating.set(false)
    });
}
```

**File: `UI/src/app/admin/users/users.component.html`**

Add before the users table:
```html
<!-- Only show for SUPER_ADMIN -->
@if (isSuperAdmin) {
  <div class="page-header-actions">
    <button mat-flat-button class="create-btn" (click)="showCreateForm.set(!showCreateForm())">
      <mat-icon>person_add</mat-icon> New User
    </button>
  </div>
}

@if (showCreateForm()) {
  <mat-card class="create-card">
    <mat-card-header><mat-card-title>Create New User</mat-card-title></mat-card-header>
    <mat-card-content class="form-row">
      <mat-form-field appearance="outline">
        <mat-label>Phone Number</mat-label>
        <input matInput type="tel" [(ngModel)]="createForm.phoneNumber" placeholder="10-digit mobile" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Username</mat-label>
        <input matInput [(ngModel)]="createForm.username" placeholder="Display name" />
      </mat-form-field>
      <mat-form-field appearance="outline">
        <mat-label>Role</mat-label>
        <mat-select [(value)]="createForm.roleId">
          @for (r of roleOptions; track r) {
            <mat-option [value]="r">{{ r }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
    </mat-card-content>
    <mat-card-actions>
      <button mat-flat-button class="create-btn" (click)="createUser()" [disabled]="creating()">
        @if (creating()) { <mat-spinner diameter="18" /> } @else { Create }
      </button>
      <button mat-button (click)="showCreateForm.set(false)">Cancel</button>
    </mat-card-actions>
  </mat-card>
}
```

---

## Task 3 — Last Logged In Tracking
**Status:** 🔴 Not Started
**Effort:** S — 4 file changes + 1 DB migration

### Backend Changes

**File: `backend/src/main/resources/db/migration/V6__add_last_login.sql`** (new file)
```sql
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
```

**File: `backend/src/main/java/com/weekendbasket/app/model/User.java`**

Add field:
```java
@Column(name = "last_login_at")
private java.time.LocalDateTime lastLoginAt;
```

**File: `backend/src/main/java/com/weekendbasket/app/service/AuthService.java`**

In `verifyOtp()` after token is generated, add:
```java
user.setLastLoginAt(java.time.LocalDateTime.now());
userRepository.save(user);
```

In `loginWithPassword()` after BCrypt verify succeeds, add same two lines.

**File: `backend/src/main/java/com/weekendbasket/app/dto/UserDto.java`**

Add `lastLoginAt` to `AdminUserResponse` record (or whatever the admin user response is called). If it's `UserResponse`, add:
```java
LocalDateTime lastLoginAt
```

Update `toAdminResponse()` in UserService to include `user.getLastLoginAt()`.

### Frontend Changes

**File: `UI/src/app/admin/services/admin.services.ts`**

Add `lastLoginAt` to `AdminUser` interface:
```typescript
export interface AdminUser {
  userId: number;
  phoneNumber: string;
  username: string;
  status: string;
  roles: string[];
  lastLoginAt: string | null;   // add this
}
```

**File: `UI/src/app/admin/users/users.component.html`**

In the users table, add a column showing last login:
```html
<ng-container matColumnDef="lastLogin">
  <th mat-header-cell *matHeaderCellDef>Last Login</th>
  <td mat-cell *matCellDef="let u">
    {{ u.lastLoginAt ? (u.lastLoginAt | date:'dd MMM, hh:mm a') : '—' }}
  </td>
</ng-container>
```

Add `'lastLogin'` to the `columns` array.

---

## Task 4 — User Full Profile View on Click
**Status:** 🔴 Not Started
**Effort:** S — 3 file changes

### Why
Admin needs to see flat number, block, email, has password status for support queries.

### Backend Changes

**File: `backend/src/main/java/com/weekendbasket/app/dto/UserDto.java`**

Verify `UserDetailResponse` record exists (add if not):
```java
public record UserDetailResponse(
    Long userId,
    String phoneNumber,
    String username,
    String status,
    boolean hasPassword,
    LocalDateTime lastLoginAt,
    List<String> roles,
    String firstName,
    String lastName,
    String email,
    String flatNumber,
    String block
) {}
```

**File: `backend/src/main/java/com/weekendbasket/app/controller/UserController.java`**

Verify `GET /users/{id}` returns full profile including `UserProfile` fields.
If it only returns basic user, update `UserService.getById()` to join profile:
```java
@Transactional(readOnly = true)
public UserDetailResponse getById(Long userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
    List<String> roles = roleAccessRepository.findByUserId(userId)
            .stream().map(ra -> ra.getRole().getRoleId()).toList();
    return new UserDetailResponse(
        user.getId(), user.getPhoneNumber(), user.getUsername(),
        user.getStatus(), user.isHasPassword(), user.getLastLoginAt(),
        roles,
        profile != null ? profile.getFirstName() : null,
        profile != null ? profile.getLastName() : null,
        profile != null ? profile.getEmail() : null,
        profile != null ? profile.getFlatNumber() : null,
        profile != null ? profile.getBlock() : null
    );
}
```

### Frontend Changes

**File: `UI/src/app/admin/users/users.component.ts`**

Add:
```typescript
selectedUser = signal<any | null>(null);
loadingDetail = signal(false);

viewUser(userId: number): void {
    this.loadingDetail.set(true);
    this.http.get<any>(`${this.base}/users/${userId}`)
        .subscribe({
            next: u => { this.selectedUser.set(u); this.loadingDetail.set(false); },
            error: () => this.loadingDetail.set(false)
        });
}

closeDetail(): void { this.selectedUser.set(null); }
```

**File: `UI/src/app/admin/users/users.component.html`**

Make each user row clickable:
```html
<tr mat-row *matRowDef="let row; columns: columns;" 
    (click)="viewUser(row.userId)" style="cursor:pointer"></tr>
```

Add detail panel below the table:
```html
@if (selectedUser(); as u) {
  <mat-card class="detail-card">
    <mat-card-header>
      <mat-card-title>{{ u.username }} — {{ u.phoneNumber }}</mat-card-title>
      <button mat-icon-button (click)="closeDetail()"><mat-icon>close</mat-icon></button>
    </mat-card-header>
    <mat-card-content>
      <div class="detail-grid">
        <div><strong>Status:</strong> {{ u.status }}</div>
        <div><strong>Has Password:</strong> {{ u.hasPassword ? 'Yes' : 'No' }}</div>
        <div><strong>Last Login:</strong> {{ u.lastLoginAt ? (u.lastLoginAt | date:'dd MMM yyyy, hh:mm a') : '—' }}</div>
        <div><strong>Email:</strong> {{ u.email || '—' }}</div>
        <div><strong>Flat:</strong> {{ u.flatNumber || '—' }}</div>
        <div><strong>Building:</strong> {{ u.block || '—' }}</div>
        <div><strong>Roles:</strong> {{ u.roles?.join(', ') }}</div>
      </div>
    </mat-card-content>
  </mat-card>
}
```

---

## Task 5 — Building Dropdown on Customer Profile
**Status:** 🔴 Not Started
**Effort:** M — 6 file changes + 1 DB migration

### Backend Changes

**File: `backend/src/main/resources/db/migration/V7__add_buildings.sql`** (new file)
```sql
CREATE TABLE building (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    area       VARCHAR(200),
    latitude   DECIMAL(10,7),
    longitude  DECIMAL(10,7),
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_on TIMESTAMP,
    updated_on TIMESTAMP
);

INSERT INTO building (name, area, latitude, longitude) VALUES
('Greater Infras Jasmine',    'HMT Swarnapuri Colony, Ameenpur',           17.5113, 78.3374),
('Greater Infras Iris',       'Greater Iris Layout, Ameenpur-Miyapur',     17.5115, 78.3368),
('Greater Infra''s Gardenia', 'Greater Iris Layout, Ameenpur-Miyapur',     17.5119, 78.3365),
('Greater Infra Bluebells',   'Ameenpur Road, HMT Swarnapuri Ext.',        17.5134, 78.3341),
('Greater Infras Honesty',    'Sree Balaji Nagar, RTC Colony, Miyapur',    17.5028, 78.3512),
('Greater Infras Aspen',      'Greater Iris Layout Phase, Ameenpur',       17.5111, 78.3371),
('Greater Aster',             'Greater Iris Layout Phase, Ameenpur',       17.5108, 78.3379),
('Greater Infra Daffodil',    'Greater Iris Layout Phase, Miyapur',        17.5117, 78.3361),
('Greater Infra Carnation',   'Greater Iris Layout Phase, Ameenpur',       17.5122, 78.3358);
```

**New Files:**
- `backend/model/Building.java` — `@Entity` with id, name, area, latitude, longitude, active
- `backend/repository/BuildingRepository.java` — `findByActiveTrue()`
- `backend/controller/BuildingController.java`:

```java
@RestController
@RequestMapping("/api/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingRepository buildingRepository;

    @GetMapping
    public ResponseEntity<List<Building>> getAll() {
        return ResponseEntity.ok(buildingRepository.findByActiveTrue());
    }
}
```

Make this endpoint **public** in `SecurityConfig.java`:
```java
.requestMatchers("/api/buildings").permitAll()
```

### Frontend Changes

**File: `UI/src/app/customer/services/customer.services.ts`** (or wherever customer API calls live)

Add:
```typescript
getBuildings(): Observable<{id: number; name: string; area: string}[]> {
    return this.http.get<any[]>(`${this.base}/buildings`);
}
```

**File: `UI/src/app/customer/profile/profile.component.ts`**

Add:
```typescript
buildings = signal<{id: number; name: string; area: string}[]>([]);

// In ngOnInit, load buildings:
this.customerService.getBuildings()
    .subscribe(b => this.buildings.set(b));
```

**File: `UI/src/app/customer/profile/profile.component.html`**

Replace the `block` text input with a select:
```html
<!-- BEFORE -->
<mat-form-field appearance="outline">
  <mat-label>Block</mat-label>
  <input matInput formControlName="block" placeholder="e.g. Block A" />
</mat-form-field>

<!-- AFTER -->
<mat-form-field appearance="outline">
  <mat-label>Building / Apartment</mat-label>
  <mat-select formControlName="block">
    @for (b of buildings(); track b.id) {
      <mat-option [value]="b.name">{{ b.name }}<span class="area-hint"> — {{ b.area }}</span></mat-option>
    }
  </mat-select>
</mat-form-field>
```

---

## Task 6 — Admin: Buildings Management Tab
**Status:** 🔴 Not Started
**Effort:** M — 4 new files + 2 existing file changes
**Depends on:** Task 5 (Building model + controller must exist)

### Backend Changes

Add CRUD to `BuildingController.java`:
```java
// Add to BuildingController
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Building> create(@RequestBody Building req) {
    req.setActive(true);
    return ResponseEntity.ok(buildingRepository.save(req));
}

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Building> update(@PathVariable Long id, @RequestBody Building req) {
    Building b = buildingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Building not found: " + id));
    b.setName(req.getName());
    b.setArea(req.getArea());
    b.setLatitude(req.getLatitude());
    b.setLongitude(req.getLongitude());
    b.setActive(req.isActive());
    return ResponseEntity.ok(buildingRepository.save(b));
}
```

### Frontend Changes

**New folder:** `UI/src/app/admin/buildings/`

**New file:** `buildings.component.ts` — standard CRUD component pattern (same as categories):
- Load all buildings on init
- Table with: name, area, lat, lng, active toggle
- Create form: name, area, latitude, longitude
- Edit inline or in form

**File: `UI/src/app/admin/admin.routes.ts`**

Add:
```typescript
{ path: 'buildings', loadComponent: () => import('./buildings/buildings.component').then(m => m.BuildingsComponent) },
```

**File: `UI/src/app/admin/layout/admin-layout.component.ts`**

Add to `navItems`:
```typescript
{ label: 'Buildings', icon: 'apartment', route: '/admin/buildings' },
```

---

## Task 7 — Snackbar with Progress Bar
**Status:** 🔴 Not Started
**Effort:** M — 2 new files + update all components

### Why
Current auto-close snackbar gives no visual feedback on how long it will stay. Progress bar makes it obvious and polished.

### New Files

**`UI/src/app/shared/components/snackbar/snackbar.component.ts`**
```typescript
import { Component, Inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';

@Component({
  selector: 'app-snackbar',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="snack-wrap">
      <span class="snack-msg">{{ data.message }}</span>
      <button class="snack-close" (click)="ref.dismiss()">✕</button>
    </div>
    <div class="snack-progress">
      <div class="snack-bar" [style.width.%]="progress" [style.background]="data.color"></div>
    </div>
  `,
  styles: [`
    .snack-wrap { display: flex; justify-content: space-between; align-items: center; padding: 8px 0 6px; }
    .snack-msg { font-family: var(--fm-font); font-size: 14px; font-weight: 600; }
    .snack-close { background: none; border: none; color: inherit; cursor: pointer; font-size: 16px; opacity: 0.7; }
    .snack-progress { height: 3px; background: rgba(255,255,255,0.2); border-radius: 2px; overflow: hidden; }
    .snack-bar { height: 100%; transition: width 100ms linear; border-radius: 2px; }
  `]
})
export class SnackbarComponent implements OnInit {
  progress = 100;
  private interval: any;

  constructor(
    @Inject(MAT_SNACK_BAR_DATA) public data: { message: string; duration: number; color: string },
    public ref: MatSnackBarRef<SnackbarComponent>
  ) {}

  ngOnInit(): void {
    const steps = this.data.duration / 100;
    const decrement = 100 / steps;
    this.interval = setInterval(() => {
      this.progress -= decrement;
      if (this.progress <= 0) {
        clearInterval(this.interval);
        this.ref.dismiss();
      }
    }, 100);
  }
}
```

**`UI/src/app/shared/services/snackbar.service.ts`**
```typescript
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SnackbarComponent } from '../components/snackbar/snackbar.component';

@Injectable({ providedIn: 'root' })
export class SnackbarService {
  private snackBar = inject(MatSnackBar);

  success(message: string, duration = 3000): void {
    this.open(message, duration, '#2e7d32');
  }

  error(message: string, duration = 4000): void {
    this.open(message, duration, '#c62828');
  }

  info(message: string, duration = 3000): void {
    this.open(message, duration, '#1565c0');
  }

  warning(message: string, duration = 3000): void {
    this.open(message, duration, '#e65100');
  }

  private open(message: string, duration: number, color: string): void {
    this.snackBar.openFromComponent(SnackbarComponent, {
      data: { message, duration, color },
      duration,
      panelClass: ['fm-snackbar'],
      horizontalPosition: 'right',
      verticalPosition: 'bottom'
    });
  }
}
```

Add to `styles.scss`:
```scss
.fm-snackbar .mdc-snackbar__surface {
  background: #212121 !important;
  color: white !important;
  border-radius: 8px !important;
  min-width: 280px !important;
  padding: 0 16px !important;
}
```

### Migration: Replace all snackbar calls

Find all `this.snackbar.open(...)` across every component and replace:

| Old | New |
|-----|-----|
| `this.snackbar.open('Saved!', 'Close', { duration: 2000 })` | `this.snackbarService.success('Saved!')` |
| `this.snackbar.open('Error message', 'Close', { duration: 3000 })` | `this.snackbarService.error('Error message')` |
| Any error handler snackbar | `this.snackbarService.error(...)` |

Files to update (all components that inject `MatSnackBar`):
- All files in `UI/src/app/admin/`
- All files in `UI/src/app/customer/`

---

## Status Tracker

| # | Feature | Status |
|---|---------|--------|
| 1 | Notification send by phone | 🔴 Not Started |
| 2 | Create new user (SUPER_ADMIN) | 🔴 Not Started |
| 3 | Last logged in | 🔴 Not Started |
| 4 | User full profile view | 🔴 Not Started |
| 5 | Building dropdown on profile | 🔴 Not Started |
| 6 | Buildings admin tab | 🔴 Not Started |
| 7 | Snackbar progress bar | 🔴 Not Started |
