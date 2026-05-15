# Cloud Deployment & Uptime Strategy

## Stack

| Layer       | Service              | Plan        | Cost                          |
|-------------|----------------------|-------------|-------------------------------|
| Backend     | Render               | Free        | $0                            |
| Database    | Neon PostgreSQL      | Free        | $0 (0.5 GB, enough for MVP)   |
| OTP SMS     | AWS SNS              | Free tier   | $0 (100 SMS/month free)       |
| Uptime Ping | UptimeRobot          | Free        | $0                            |

---

## AWS SNS SMS — Cost Analysis

**Free tier: 100 SMS/month — resets every month, forever. Not a trial.**

| Usage | SMS/month | Cost |
|---|---|---|
| 200 users, 1 login/month each | 200 | $2 = ₹166 (100 free + 100 paid) |
| 200 users, 1 login/week each | 800 | $14 = ₹1,160 |
| MVP realistic (users don't re-login often, JWT valid 10hrs) | ~50-80 | **$0 — within free tier** |

**Price per SMS beyond free tier: $0.02 = ₹1.66 per SMS (India)**

**Verdict: Practically free for MVP. Even at scale, very cheap.**

---

## OTP Provider Strategy

Toggle via `application.properties` — no code change needed:

```properties
# Development — free forever, OTP appears in server logs
otp.provider=dev

# Production — real SMS via AWS SNS
otp.provider=sns
```

| Provider | When to use | Cost |
|---|---|---|
| `dev` | Local development, backend API testing | Free forever |
| `sns` | UAT with real phones, production | 100 free/month, $0.00645 after |

> ⚠️ **Important:** The SNS send logic in `AwsSnsOtpStrategy.java` is currently commented out to prevent accidental charges during development.
> To re-enable for production: uncomment the `send()` method body in `AwsSnsOtpStrategy.java` and set `otp.provider=sns`.

---

## AWS SNS Setup Steps

### 1. Create IAM User for SNS

1. Go to **AWS Console → IAM → Users → Create User**
2. Username: `weekendbasket-sns`
3. Attach policy: `AmazonSNSFullAccess` (or create custom with `sns:Publish` only)
4. Create access key → copy `Access Key ID` and `Secret Access Key`

### 2. Enable SMS in AWS SNS

1. Go to **AWS Console → SNS → Text messaging (SMS)**
2. Set default SMS type to `Transactional` (higher delivery priority, not filtered by DND)
3. Region: `ap-south-1` (Mumbai — closest to India, lowest latency)

### 3. Set Environment Variables

**In Render dashboard (production):**
```
otp.provider          = sns
AWS_ACCESS_KEY_ID     = <your-iam-access-key>
AWS_SECRET_ACCESS_KEY = <your-iam-secret>
AWS_SNS_REGION        = ap-south-1
```

**In application-local.properties (local testing):**
```properties
otp.provider=sns
AWS_ACCESS_KEY_ID=<your-iam-access-key>
AWS_SECRET_ACCESS_KEY=<your-iam-secret>
AWS_SNS_REGION=ap-south-1
```

### 4. SMS Message Format

OTP SMS sent to user:
```
Your WeekendBasket OTP is: 123456. Valid for 5 minutes. Do not share.
```

Sender ID shown on phone: `WKNDBASKT`

---

## The Sleep Problem

Both Render (backend) and Neon (database) sleep after inactivity on free tier:

| Service | Sleeps after | Wake-up time | Impact                           |
|---------|--------------|--------------|----------------------------------|
| Render  | 15 min       | ~30 seconds  | First API call is slow           |
| Neon    | 5 min        | ~500ms       | First DB query after idle is slow|

**Worst case:** Both asleep → first request takes ~30 seconds.
**Normal case:** UptimeRobot keeps both awake → no cold start.

**Critical impact without ping:**
- Monday 00:00 scheduler (open cycle) → Render is asleep → never fires
- Wednesday 14:00 scheduler (close cycle) → same problem
- Admin manually opens/closes as workaround for MVP

---

## Solution — UptimeRobot Ping

### Setup Steps

1. Go to **uptimerobot.com** → Sign up free
2. Click **Add New Monitor**
3. Fill in:
   - Monitor Type: `HTTP(s)`
   - Friendly Name: `WeekendBasket API`
   - URL: `https://<your-render-url>/weekendbasket/api/health`
   - Monitoring Interval: `5 minutes`
4. Click **Create Monitor**

UptimeRobot pings `/api/health` every 5 minutes:
- Render backend stays awake
- Every ping hits the DB → Neon stays awake
- Schedulers fire reliably
- No cold starts for users

---

## Render Deployment Steps

### 1. Push code to GitHub
```
git init
git add .
git commit -m "initial commit"
git remote add origin https://github.com/<you>/weekendbasket-backend.git
git push -u origin main
```

### 2. Create Render Web Service
1. Go to **render.com** → New → Web Service
2. Connect your GitHub repo
3. Fill in:
   - Name: `weekendbasket-backend`
   - Runtime: `Java`
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/app-0.0.1-SNAPSHOT.jar`
   - Instance Type: `Free`

### 3. Set Environment Variables in Render Dashboard
```
DB_URL                = jdbc:postgresql://<neon-host>/neondb?sslmode=require
DB_USERNAME           = <neon-username>
DB_PASSWORD           = <neon-password>
JWT_SECRET            = <strong-random-secret>
otp.provider          = sns
AWS_ACCESS_KEY_ID     = <iam-access-key>
AWS_SECRET_ACCESS_KEY = <iam-secret>
AWS_SNS_REGION        = ap-south-1
```

### 4. Deploy
Render auto-deploys on every push to `main`.

---

## Neon Connection Details

Host: `ep-young-pond-aonkktkw.c-2.ap-southeast-1.aws.neon.tech`
Database: `neondb`
JDBC URL: `jdbc:postgresql://ep-young-pond-aonkktkw.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require`

**Storage used:** ~31 MB (PostgreSQL baseline overhead — normal)
**Storage limit:** 512 MB
**Headroom:** ~480 MB (enough for 12+ months of MVP data)

---

## Flyway Migration Behaviour

On first deploy:
1. Creates `flyway_schema_history` table
2. Runs `V1__init_schema.sql` → all 20 tables created
3. Runs `V2__seed_data.sql` → roles + master data seeded
4. App ready

On subsequent deploys:
- Only new migration files run (V3, V4 etc.)
- Existing data untouched
- Never edit V1 or V2 — Flyway checksums them

---

## TODO — Before Production

- [ ] Create IAM user `farmmitra-sns` with `sns:Publish` permission
- [ ] Enable SNS SMS in ap-south-1 region, set type to Transactional
- [ ] Set up UptimeRobot monitor on `/api/health`
- [ ] Push code to GitHub
- [ ] Create Render Web Service (root dir: `backend`)
- [ ] Set all env vars in Render dashboard (DB + JWT + SNS + CORS)
- [ ] Create Netlify site (base dir: `UI`, publish: `dist/weekendbasket-ui/browser`)
- [ ] Update `environment.prod.ts` with actual Render URL
- [ ] Update `CORS_ALLOWED_ORIGINS` in Render with actual Netlify URL
- [ ] Verify first deploy — check Render logs for Flyway migration success
- [ ] Test `POST /api/auth/send-otp` → OTP in Render logs (dev mode)
- [ ] Rotate JWT_SECRET to a strong random value before go-live

> Full step-by-step instructions: see `docs/19-deployment-guide.md`
