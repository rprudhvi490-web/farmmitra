# FarmMitra — Deployment Guide
## GitHub → Render (Backend) → Netlify (Frontend)
### 100% Free Tier

---

## Architecture Overview

```
GitHub (code)
    ├── backend/   → Render (Spring Boot JAR)   → free.render.com
    └── UI/        → Netlify (Angular SPA)       → netlify.app

Database: Neon PostgreSQL (already running, free tier)
OTP SMS:  AWS SNS (100 free SMS/month)
Uptime:   UptimeRobot (keeps Render awake, free)
```

---

## Step 1 — Prepare GitHub Repository

### 1.1 Install Git (if not already)
Download from https://git-scm.com/download/win and install with defaults.

### 1.2 Create GitHub account
Go to https://github.com and sign up if you don't have an account.

### 1.3 Create a new repository on GitHub
1. Click **+** → **New repository**
2. Name: `farmmitra` (or any name you prefer)
3. Visibility: **Private** (recommended — contains your app code)
4. Do NOT initialize with README (you already have code)
5. Click **Create repository**

### 1.4 Push your code from `C:\WeekendBasket`

Open Command Prompt and run:

```cmd
cd C:\WeekendBasket
git init
git add .
git commit -m "initial commit — FarmMitra full stack"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/farmmitra.git
git push -u origin main
```

> Replace `YOUR_USERNAME` with your actual GitHub username.

**Verify:** Go to your GitHub repo — you should see `backend/`, `UI/`, `docs/` folders.

---

## Step 2 — Deploy Backend to Render (Free)

### 2.1 Create Render account
Go to https://render.com → Sign up with GitHub (easiest — auto-connects repos).

### 2.2 Create a Web Service

1. Click **New +** → **Web Service**
2. Connect your GitHub repo `farmmitra`
3. Fill in the settings:

| Field | Value |
|-------|-------|
| Name | `farmmitra-backend` |
| Region | Singapore (closest to India) |
| Branch | `main` |
| Root Directory | `backend` |
| Runtime | `Java` |
| Build Command | `./mvnw clean package -DskipTests` |
| Start Command | `java -jar target/app-0.0.1-SNAPSHOT.jar` |
| Instance Type | **Free** |

4. Click **Create Web Service**

### 2.3 Set Environment Variables in Render

Go to your service → **Environment** tab → Add the following:

| Key | Value |
|-----|-------|
| `DB_URL` | `jdbc:postgresql://YOUR_NEON_HOST/neondb?sslmode=require` |
| `DB_USERNAME` | your Neon username |
| `DB_PASSWORD` | your Neon password |
| `JWT_SECRET` | generate a strong random string (min 32 chars) |
| `otp.provider` | `dev` (change to `sns` when ready for real SMS) |
| `AWS_ACCESS_KEY_ID` | your IAM access key (or `not-configured` for now) |
| `AWS_SECRET_ACCESS_KEY` | your IAM secret (or `not-configured` for now) |
| `AWS_SNS_REGION` | `ap-south-1` |
| `CORS_ALLOWED_ORIGINS` | `https://YOUR_NETLIFY_APP.netlify.app` ← fill after Step 3 |

> **JWT_SECRET tip:** Generate one at https://generate-secret.vercel.app/64

### 2.4 Wait for first deploy

Render will:
1. Pull your code from GitHub
2. Run `./mvnw clean package -DskipTests` (~3-5 minutes first time)
3. Start the JAR
4. Flyway runs migrations → all 20 tables created automatically

**Check logs:** Click **Logs** tab in Render. You should see:
```
Started WeekendBasketApplication in X seconds
Flyway: Successfully applied 2 migrations
```

### 2.5 Note your Render URL

It will look like: `https://farmmitra-backend.onrender.com`

Test it: Open `https://farmmitra-backend.onrender.com/weekendbasket/api/health`
Expected response: `{"status":"UP"}`

---

## Step 3 — Deploy Frontend to Netlify (Free)

### 3.1 Update production API URL

Before deploying, update `UI/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiBaseUrl: 'https://farmmitra-backend.onrender.com/weekendbasket/api'
};
```

Commit and push this change:
```cmd
cd C:\WeekendBasket
git add UI/src/environments/environment.prod.ts
git commit -m "set production API URL"
git push
```

### 3.2 Create Netlify account
Go to https://netlify.com → Sign up with GitHub.

### 3.3 Create a new site from Git

1. Click **Add new site** → **Import an existing project**
2. Connect GitHub → select your `farmmitra` repo
3. Fill in build settings:

| Field | Value |
|-------|-------|
| Base directory | `UI` |
| Build command | `npm run build` |
| Publish directory | `UI/dist/weekendbasket-ui/browser` |

4. Click **Deploy site**

### 3.4 Note your Netlify URL

It will look like: `https://farmmitra-abc123.netlify.app`

You can rename it: **Site settings** → **Change site name** → e.g. `farmmitra`
→ URL becomes `https://farmmitra.netlify.app`

### 3.5 Update CORS on Render

Go back to Render → your service → **Environment** tab.
Update `CORS_ALLOWED_ORIGINS`:
```
https://farmmitra.netlify.app
```

Click **Save Changes** — Render will redeploy automatically.

---

## Step 4 — Set Up UptimeRobot (Keep Render Awake)

Render free tier sleeps after 15 minutes of inactivity. UptimeRobot pings it every 5 minutes.

1. Go to https://uptimerobot.com → Sign up free
2. Click **Add New Monitor**
3. Fill in:
   - Monitor Type: `HTTP(s)`
   - Friendly Name: `FarmMitra API`
   - URL: `https://farmmitra-backend.onrender.com/weekendbasket/api/health`
   - Monitoring Interval: `5 minutes`
4. Click **Create Monitor**

**Result:** Render stays awake 24/7. Schedulers (Monday open, Wednesday close) fire reliably.

---

## Step 5 — Verify End-to-End

Test the full flow after deployment:

```
1. Open https://farmmitra.netlify.app
2. Login with OTP (OTP appears in Render logs since otp.provider=dev)
3. Admin creates a cycle
4. Customer places an order
5. Admin views procurement sheet
6. Admin marks order delivered
7. Check dashboard for revenue stats
```

To see OTP in dev mode: Render → **Logs** → search for `OTP for`

---

## Step 6 — Custom Domain (Optional, Free)

If you have a domain (e.g. from GoDaddy or Namecheap):

**Netlify:**
1. Site settings → Domain management → Add custom domain
2. Add a CNAME record in your DNS: `www` → `farmmitra.netlify.app`
3. Netlify auto-provisions SSL (Let's Encrypt)

**Update CORS on Render:**
```
CORS_ALLOWED_ORIGINS=https://www.farmmitra.in,https://farmmitra.netlify.app
```

---

## Step 7 — Enable Real SMS (When Ready)

When you want real OTP SMS instead of log-based:

1. Create IAM user in AWS Console → IAM → Users
   - Username: `farmmitra-sns`
   - Policy: `AmazonSNSFullAccess`
   - Create access key → copy both values

2. Enable SMS in AWS SNS:
   - Go to SNS → Text messaging (SMS)
   - Set type to `Transactional`
   - Region: `ap-south-1` (Mumbai)

3. Update Render environment variables:
   ```
   otp.provider          = sns
   AWS_ACCESS_KEY_ID     = <your-key>
   AWS_SECRET_ACCESS_KEY = <your-secret>
   ```

4. Render redeploys automatically. Test with a real phone number.

**Cost:** 100 SMS/month free forever. Beyond that: ₹1.66/SMS.

---

## Auto-Deploy on Every Push

Both Render and Netlify watch your `main` branch.

```
git add .
git commit -m "your change"
git push
```

→ Render rebuilds and redeploys the backend automatically
→ Netlify rebuilds and redeploys the frontend automatically

No manual steps needed after initial setup.

---

## Free Tier Limits Summary

| Service | Free Limit | Impact |
|---------|-----------|--------|
| Render | 750 hrs/month, sleeps after 15min idle | UptimeRobot solves sleep |
| Neon | 512 MB storage, 1 project | Enough for 12+ months of MVP data |
| Netlify | 100 GB bandwidth/month, 300 build mins | More than enough |
| UptimeRobot | 50 monitors, 5-min interval | Free forever |
| AWS SNS | 100 SMS/month | Free forever |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Render build fails | Check logs — usually missing env var or Maven wrapper permission |
| `./mvnw` permission denied | Add `chmod +x backend/mvnw` as pre-build step in Render |
| CORS error in browser | Check `CORS_ALLOWED_ORIGINS` matches your exact Netlify URL |
| Angular routes 404 on refresh | `_redirects` file in `UI/public/` handles this — already in place |
| OTP not arriving | Check Render logs for OTP value (dev mode) or SNS config (prod mode) |
| Render sleeping despite UptimeRobot | Verify monitor URL returns 200, not 404 |
| Flyway migration fails | Never edit V1/V2 SQL files — add new V3, V4 files for schema changes |

---

## Environment Variables Reference

### Render (Backend)

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | Neon JDBC URL | `jdbc:postgresql://host/neondb?sslmode=require` |
| `DB_USERNAME` | Neon username | `neondb_owner` |
| `DB_PASSWORD` | Neon password | `<secret>` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | `<random-64-char-string>` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `https://farmmitra.netlify.app` |
| `otp.provider` | `dev` or `sns` | `dev` |
| `AWS_ACCESS_KEY_ID` | IAM access key for SNS | `AKIA...` |
| `AWS_SECRET_ACCESS_KEY` | IAM secret for SNS | `<secret>` |
| `AWS_SNS_REGION` | SNS region | `ap-south-1` |

### Netlify (Frontend)

No environment variables needed — API URL is baked into the production build via `environment.prod.ts`.
