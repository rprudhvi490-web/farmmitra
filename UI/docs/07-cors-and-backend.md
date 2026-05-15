# CORS & Backend Configuration

## The Problem

Angular dev server runs on `http://localhost:4200`.
Spring Boot backend runs on `http://localhost:9090`.

Browsers block cross-origin requests unless the backend explicitly allows them.

---

## Solution 1 — Angular Proxy (Development Only)

`proxy.conf.json` in `C:\WeekendBasket\UI\`:

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

Angular dev server forwards all `/weekendbasket/**` requests to `localhost:9090`.
Browser sees requests going to `localhost:4200` — no CORS issue.

Register in `angular.json`:
```json
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

With proxy, `environment.ts` uses relative path:
```typescript
apiBaseUrl: '/weekendbasket/api'
```

---

## Solution 2 — Backend CORS Config ✅ Implemented

In `SecurityConfig.java` — reads allowed origins from `CORS_ALLOWED_ORIGINS` environment variable:

```java
@Value("${cors.allowed.origins:http://localhost:4200}")
private String allowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

In `application.properties`:
```properties
cors.allowed.origins=${CORS_ALLOWED_ORIGINS:http://localhost:4200}
```

In Render environment variables (production):
```
CORS_ALLOWED_ORIGINS=https://farmmitra.netlify.app
```

For multiple origins (custom domain + Netlify):
```
CORS_ALLOWED_ORIGINS=https://farmmitra.netlify.app,https://www.farmmitra.in
```

---

## Backend Base URL

| Environment | Angular calls | Backend serves |
|-------------|---------------|----------------|
| Dev (proxy) | `/weekendbasket/api/...` | `localhost:9090/weekendbasket/api/...` |
| Production  | `https://<render-url>/weekendbasket/api/...` | Render deployment |

---

## Backend Context Path

Spring Boot is configured with:
```properties
server.servlet.context-path=/weekendbasket
```

So all API paths are: `http://localhost:9090/weekendbasket/api/{endpoint}`

---

## Checklist Before Running Angular

- [ ] Spring Boot backend running on port 9090
- [ ] `proxy.conf.json` in place
- [ ] `ng serve --proxy-config proxy.conf.json` (or registered in `angular.json`)
- [ ] Test: `GET http://localhost:4200/weekendbasket/api/health` returns `{ "status": "UP" }`
